package org.eientei.videostreamer.backend.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.videostreamer.backend.orm.entity.Message;
import org.eientei.videostreamer.backend.orm.entity.Stream;
import org.eientei.videostreamer.backend.orm.service.MessageService;
import org.eientei.videostreamer.backend.orm.service.StreamService;
import org.eientei.videostreamer.backend.pojo.chat.*;
import org.eientei.videostreamer.backend.utils.VideostreamerUtils;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * User: iamtakingiteasy
 * Date: 2015-01-20
 * Time: 10:43
 */
public class ChatRoom {
    private MessageService messageService;
    private Set<ChatClient> clients = new ConcurrentSkipListSet<ChatClient>();
    private Map<ChatClient, Long> typers = new ConcurrentHashMap<ChatClient, Long>();
    private ObjectMapper mapper = new ObjectMapper();
    private Stream stream;
    private StreamService streamService;

    public ChatRoom(Stream stream, StreamService streamService, MessageService messageService) {
        this.stream = stream;
        this.streamService = streamService;
        this.messageService = messageService;
    }

    public void tick() {
        long ref = System.currentTimeMillis();

        Iterator<Long> iterator = typers.values().iterator();

        boolean updated = false;

        while (iterator.hasNext()) {
            long it = iterator.next();
            if (ref - it > 1000) {
                iterator.remove();
                updated = true;
            }
        }

        if (updated) {
            deliverTypingUpdate();
        }
    }

    private void deliverOnlineUpdate() {
        ServerChat builder = new ServerChat();
        builder.setType(ChatMessageType.ONLINE);

        ServerChatOnline online = new ServerChatOnline();
        online.getItems().addAll(getOnlines());
        builder.setData(online);
        broadcastMessage(builder);
    }

    private void deliverTypingUpdate() {
        ServerChat builder = new ServerChat();
        builder.setType(ChatMessageType.TYPING);

        ServerChatTyping typing = new ServerChatTyping();

        for (ChatClient client : typers.keySet()) {
            typing.getItems().add(VideostreamerUtils.determineUserHash(client.getAppUserDetails().getDataUser(), client.getRemote()));
        }
        builder.setData(typing);
        broadcastMessage(builder);
    }

    private void deliverHistoryUpdate(ChatClient client, long refpoint) {
        ServerChat builder = new ServerChat();
        builder.setType(ChatMessageType.HISTORY);

        ServerChatHistory history = new ServerChatHistory();

        List<Message> messages = messageService.loadLast(getStream(), refpoint, 31);
        if (messages.size() == 31) {
            history.setHasMore(true);
            messages.remove(30);
        } else {
            history.setHasMore(false);
        }

        for (Message message : messages) {
            history.getItems().add(VideostreamerUtils.buildMessageItem(message));
        }

        builder.setData(history);
        deliverMessage(client, makeTextMessage(builder));
    }

    private void broadcastMessage(ServerChat build) {
        TextMessage message = makeTextMessage(build);
        for (ChatClient client : clients) {
            deliverMessage(client, message);
        }
    }

    private void deliverMessage(ChatClient client, TextMessage message) {
        try {
            client.getSession().sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClient(ChatClient client) {
        clients.remove(client);
        deliverOnlineUpdate();
    }

    public void addClient(ChatClient client) {
        clients.add(client);

        deliverOnlineUpdate();
        deliverTypingUpdate();
    }

    public void relayMessage(ChatClient client, String text) {
        Message message = new Message();
        message.setAdmin(false);
        message.setAuthor(client.getAppUserDetails().getDataUser());
        message.setAuthor(message.getAuthor().getId() == getStream().getAuthor().getId());
        message.setMessage(text);
        message.setPosted(new Date());
        message.setRemote(client.getRemote());
        message.setStream(getStream());
        messageService.saveMessage(message);

        ServerChat builder = new ServerChat();
        builder.setType(ChatMessageType.MESSAGE);
        builder.setData(VideostreamerUtils.buildMessageItem(message));
        broadcastMessage(builder);
    }

    private TextMessage makeTextMessage(ServerChat build) {
        try {
            return new TextMessage(mapper.writeValueAsString(build));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void sendHistory(ChatClient client, long refpoint) {
        deliverHistoryUpdate(client, refpoint);
    }

    public void markTyper(ChatClient client) {
        boolean was = typers.containsKey(client);
        typers.put(client, System.currentTimeMillis());

        if (!was) {
            deliverTypingUpdate();
        }
    }

    public void sendTopic() {
        ServerChatTopic topicBuilder = new ServerChatTopic();

        topicBuilder.setTopic(getStream().getTopic());

        ServerChat builder = new ServerChat();
        builder.setType(ChatMessageType.TOPIC);
        builder.setData(topicBuilder);

        broadcastMessage(builder);
    }

    public Stream getStream() {
        streamService.refresh(stream);
        return stream;
    }

    public void sendOnline() {
        deliverOnlineUpdate();
    }

    public Collection<? extends ServerChatOnlineItem> getOnlines() {
        List<ServerChatOnlineItem> items = new ArrayList<ServerChatOnlineItem>();
        for (ChatClient client : clients) {
            ServerChatOnlineItem item = new ServerChatOnlineItem();
            item.setHash(VideostreamerUtils.determineUserHash(client.getAppUserDetails().getDataUser(), client.getRemote()));
            item.setName(client.getAppUserDetails().getUsername());
            item.setOwner(client.getAppUserDetails().getDataUser() != null && client.getAppUserDetails().getDataUser().getId() == getStream().getAuthor().getId());
            items.add(item);
        }
        return items;
    }
}
