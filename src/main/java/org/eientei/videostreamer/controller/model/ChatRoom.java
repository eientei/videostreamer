package org.eientei.videostreamer.controller.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.videostreamer.dto.*;
import org.eientei.videostreamer.orm.entity.Stream;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 14:14
 */
public class ChatRoom {
    private static Comparator<ChatMessageDTO> comparator = new Comparator<ChatMessageDTO>() {
        @Override
        public int compare(ChatMessageDTO o1, ChatMessageDTO o2) {
            return o1.getId().compareTo(o2.getId());
        }
    };

    private Stream stream;
    private List<ChatClient> clients = new CopyOnWriteArrayList<ChatClient>();
    private ObjectMapper mapper = new ObjectMapper();
    private List<ChatClient> prevtypers = new ArrayList<ChatClient>();
    private TreeSet<ChatMessageDTO> messages = new TreeSet<ChatMessageDTO>(comparator);
    private TreeSet<ChatMessageDTO> ceils = new TreeSet<ChatMessageDTO>(comparator);
    private boolean bottom;

    public ChatRoom(Stream stream) {
        this.stream = stream;
    }

    public void removeClient(ChatClient client) {
        clients.remove(client);
        updateOnline();
    }

    public void addClient(ChatClient client) {
        clients.add(client);
        updateOnline();
    }

    public void updateOnline() {
        ChatOnlineDTO message = new ChatOnlineDTO(clients);
        broadcastMessaage(message);
    }

    public void bootstrapTypers(ChatClient dest) {
        long time = System.currentTimeMillis();
        List<ChatClient> typers = new ArrayList<ChatClient>();
        for (ChatClient client : clients) {
            if (client.isTyping(time)) {
                typers.add(client);
            }
        }
        ChatTypingDTO message = new ChatTypingDTO(typers);
        deliverMessage(dest, makeTextMessage(message));
    }

    public void updateTypers() {
        long time = System.currentTimeMillis();
        List<ChatClient> typers = new ArrayList<ChatClient>();
        boolean update = false;
        for (ChatClient client : clients) {
            if (client.isTyping(time)) {
                typers.add(client);
                if (!prevtypers.contains(client)) {
                    update = true;
                }
            } else {
                if (prevtypers.contains(client)) {
                    update = true;
                }
            }
        }
        if (update) {
            ChatTypingDTO message = new ChatTypingDTO(typers);
            prevtypers = typers;
            broadcastMessaage(message);
        }
    }

    public void updateTopic() {
        ChatTopicDTO message = new ChatTopicDTO(stream.getTopic());
        broadcastMessaage(message);
    }

    public void updateImage() {
        ChatImageDTO message = new ChatImageDTO(stream.getIdleImage());
        broadcastMessaage(message);
    }

    private void broadcastMessaage(Object obj) {
        TextMessage message = makeTextMessage(obj);
        for (ChatClient client : clients) {
            deliverMessage(client, message);
        }
    }

    private void deliverMessage(ChatClient client, TextMessage message) {
        try {
            client.getSession().sendMessage(message);
        } catch (IOException ignore) {
        }
    }

    private TextMessage makeTextMessage(Object obj) {
        try {
            return new TextMessage(mapper.writeValueAsString(new ChatMessageDispatcher(obj)));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public List<ChatClient> getOnline() {
        return clients;
    }

    public void tick() {
        updateTypers();
    }

    public void broadcast(Object messageDTO) {
        broadcastMessaage(messageDTO);
    }

    public void deliver(Object messageDTO, ChatClient client) {
        deliverMessage(client, makeTextMessage(messageDTO));
    }

    public Stream getStream() {
        return stream;
    }

    public TreeSet<ChatMessageDTO> getMessages() {
        return messages;
    }

    public TreeSet<ChatMessageDTO> getCeils() {
        return ceils;
    }

    public boolean isBottom() {
        return bottom;
    }

    public void setBottom(boolean bottom) {
        this.bottom = bottom;
    }

    public void destroy() {
        for (ChatClient client : clients) {
            try {
                client.getSession().close();
            } catch (Exception ignore) {
            }
        }
    }

    public void migrate(String newname) {
        ChatMigrateDTO message = new ChatMigrateDTO(newname);
        broadcast(message);
    }
}
