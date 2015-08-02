package org.eientei.video.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.video.backend.config.security.VideostreamerUser;
import org.eientei.video.backend.controller.model.ChatClient;
import org.eientei.video.backend.controller.model.ChatRoom;
import org.eientei.video.backend.dto.*;
import org.eientei.video.backend.orm.entity.Message;
import org.eientei.video.backend.orm.entity.Stream;
import org.eientei.video.backend.orm.service.MessageService;
import org.eientei.video.backend.orm.service.StreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 13:46
 */
@Component
public class Chat extends TextWebSocketHandler {
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Map<String, ChatRoom> rooms = new ConcurrentHashMap<String, ChatRoom>();
    private Map<String, ChatClient> clients = new ConcurrentHashMap<String, ChatClient>();
    private Map<String, String> sessions = new ConcurrentHashMap<String, String>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private AtomicLong anonymousId = new AtomicLong(0);

    @Autowired
    private StreamService streamService;

    @Autowired
    private MessageService messageService;

    @PostConstruct
    public void postConstruct() {
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                for (ChatRoom room : rooms.values()) {
                    room.tick();
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        VideostreamerUser user = (VideostreamerUser) session.getAttributes().get("principal");
        String sessionId = (String) session.getAttributes().get("sessionid");
        ChatClient client = new ChatClient(session, user);
        clients.put(session.getId(), client);
        sessions.put(sessionId, session.getId());
    }

    public void refreshSession(String sessionId, String id, VideostreamerUser user) {
        if (sessionId == null) {
            return;
        }
        String clientId = sessions.remove(sessionId);
        if (clientId == null) {
            return;
        }
        sessions.put(id, clientId);
        ChatClient client = clients.get(clientId);
        if (client == null) {
            return;
        }
        client.setUser(user);
        ChatRoom room = client.getRoom();
        room.updateOnline();
        room.deliver(new ChatInfoDTO(client.isOwner(), room.getStream().getTopic(), room.getStream().getIdleImage()), client);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionid");
        sessions.remove(sessionId);
        ChatClient client = clients.remove(session.getId());
        ChatRoom room = client.getRoom();
        if (room != null) {
            room.removeClient(client);
        }

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatClient client = clients.get(session.getId());
        try {
            ChatMessageDispatcher dispatcher = objectMapper.readValue(message.getPayload(), ChatMessageDispatcher.class);
            switch (dispatcher.getType()) {
                case CONNECT:
                    ChatConnectDTO connectDTO = dispatcher.dispatch();
                    handleChatConnect(connectDTO, client);
                    break;
                case ONLINE:
                    // ignore
                    break;
                case TYPING:
                    // ignore
                    break;
                case HISTORY:
                    // ignore
                    break;
                case PREVIEW:
                    // ignore
                    break;
                case IMAGE:
                    // ignore
                    break;
                case MIGRATE:
                    // ignore
                    break;
                case INFO:
                    // ignore
                    break;
                case KEYPRESS:
                    handleChatKeypress(client);
                    break;
                case MESSAGE:
                    ChatMessageDTO messageDTO = dispatcher.dispatch();
                    handleChatMessage(messageDTO, client);
                    break;
                case HISTORYREQUEST:
                    ChatHistoryRequestDTO historyRequestDTO = dispatcher.dispatch();
                    handleChatHistory(historyRequestDTO, client, false);
                    break;
                case PREVIEWREQUEST:
                    ChatPreviewRequestDTO previewRequestDTO = dispatcher.dispatch();
                    handleChatPreview(previewRequestDTO, client);
                    break;
                case TOPIC:
                    ChatTopicDTO topicDTO = dispatcher.dispatch();
                    handleChatTopic(topicDTO, client);
                    break;
            }
        } catch (Exception e) {
            client.getSession().close();
        }
    }

    private void handleChatTopic(ChatTopicDTO topicDTO, ChatClient client) {
        if (client.isOwner()) {
            Stream stream = client.getRoom().getStream();
            streamService.updateStreamTopic(stream, topicDTO.getTopic());
        }
    }

    private void handleChatMessage(ChatMessageDTO messageDTO, ChatClient client) {
        if (messageDTO.getText().trim().length() == 0) {
            return;
        }

        ChatRoom room = client.getRoom();
        Stream stream = room.getStream();

        Message message = messageService.persist(stream, messageDTO.getText(), client.getUser().getEntity(), client.isOwner(), client.getRemote());
        ChatMessageDTO dto = new ChatMessageDTO(message);
        room.getMessages().add(dto);
        room.broadcast(dto);
    }

    private void handleChatHistory(ChatHistoryRequestDTO historyRequestDTO, ChatClient client, boolean inclusive) {
        if (historyRequestDTO.getId() == null) {
            return;
        }

        int max = 30;
        long id = historyRequestDTO.getId();

        ChatHistoryDTO history = new ChatHistoryDTO();

        ChatMessageDTO ref = new ChatMessageDTO(historyRequestDTO.getId());

        ChatRoom room = client.getRoom();

        SortedSet<ChatMessageDTO> headSet = room.getMessages().headSet(ref, false).descendingSet();

        if (inclusive) {
            max -= 1;
        }

        for (ChatMessageDTO dto : headSet) {
            history.getHistory().addFirst(dto);
            if (dto.getId() < id) {
                id = dto.getId();
            }
            if (--max <= 0) {
                break;
            }
        }

        if (inclusive) {
            history.getHistory().addLast(room.getMessages().ceiling(ref));
        }

        if (max > 0 && !room.isBottom()) {
            for (Message message : messageService.from(room.getStream(), max+1, id)) {
                ChatMessageDTO dto = new ChatMessageDTO(message);
                room.getMessages().add(dto);
                if (--max >= 0) {
                    history.getHistory().addFirst(dto);
                }
            }
            if (max >= 0) {
                room.setBottom(true);
            }
        }

        history.setMore(max <= 0 && room.getMessages().lower(ref) != null);

        room.deliver(history, client);
    }

    private void handleChatPreview(ChatPreviewRequestDTO previewRequestDTO, ChatClient client) {
        if (previewRequestDTO.getId() == null) {
            return;
        }
        ChatPreviewDTO previewDTO = new ChatPreviewDTO();
        ChatRoom room = client.getRoom();
        ChatMessageDTO ceiling = room.getCeils().ceiling(new ChatMessageDTO(previewRequestDTO.getId()));
        if (ceiling == null || !ceiling.getId().equals(previewRequestDTO.getId())) {
            Message message = messageService.load(room.getStream(), previewRequestDTO.getId());
            if (message != null) {
                ceiling = new ChatMessageDTO(message);
                room.getCeils().add(ceiling);
                previewDTO.setPreview(ceiling);
            }
        } else {
            previewDTO.setPreview(ceiling);
        }
        previewDTO.setId(previewRequestDTO.getId());
        room.deliver(previewDTO, client);
    }

    private void handleChatKeypress(ChatClient client) {
        client.markTyping();
        client.getRoom().updateTypers();
    }

    private void handleChatConnect(ChatConnectDTO connectDTO, ChatClient client) throws IOException {
        Stream stream = streamService.getStream(connectDTO.getApp(), connectDTO.getName());
        String appName = connectDTO.getApp() + '/' + connectDTO.getName();
        if (stream == null) {
            client.getSession().close();
            return;
        }

        ChatRoom room = rooms.get(appName);
        if (room == null) {
            room = new ChatRoom(stream);
            rooms.put(appName, room);
            List<ChatMessageDTO> messageDTOs = new ArrayList<ChatMessageDTO>();
            for (Message message : messageService.last(stream, 30)) {
                messageDTOs.add(new ChatMessageDTO(message));
            }
            room.getMessages().addAll(messageDTOs);
        }

        client.setRoom(room);
        room.addClient(client);

        room.deliver(new ChatInfoDTO(client.isOwner(), room.getStream().getTopic(), room.getStream().getIdleImage()), client);

        if (!room.getMessages().isEmpty()) {
            ChatHistoryRequestDTO historyRequestDTO = new ChatHistoryRequestDTO();
            historyRequestDTO.setId(room.getMessages().last().getId());
            handleChatHistory(historyRequestDTO, client, true);
        }
    }

    public void migrateRoom(String app, String name, String newname) {
        ChatRoom room = rooms.remove(app + '/' + name);
        if (room != null) {
            rooms.put(app + '/' + newname, room);
            room.migrate(newname);
        }
    }

    public ChatRoom getRoom(String app, String name) {
        return rooms.get(app + '/' + name);
    }

    public List<ChatRoom> getRooms(VideostreamerUser user) {
        List<ChatRoom> out = new ArrayList<ChatRoom>();
        for (ChatRoom room : rooms.values()) {
            for (ChatClient client : room.getOnline()) {
                if (client.getUser().getEntity().getId().equals(user.getEntity().getId())) {
                    out.add(room);
                    break;
                }
            }
        }
        return out;
    }
}
