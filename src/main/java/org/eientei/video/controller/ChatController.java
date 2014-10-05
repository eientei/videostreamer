package org.eientei.video.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.video.data.*;
import org.eientei.video.orm.entity.Message;
import org.eientei.video.orm.entity.Stream;
import org.eientei.video.orm.service.MessageService;
import org.eientei.video.orm.service.StreamService;
import org.eientei.video.orm.service.UserService;
import org.eientei.video.security.AppUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-15
 * Time: 13:05
 */
@Component
public class ChatController extends TextWebSocketHandler {
    @Autowired
    private StreamService streamService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private MessageService messageService;

    private Map<Integer, ChatMessage> sparseMessages = new ConcurrentHashMap<Integer, ChatMessage>();
    private Map<String, ChatClient> clients = new ConcurrentHashMap<String, ChatClient>();
    private Map<String, ChatRoom> rooms = new ConcurrentHashMap<String, ChatRoom>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public ChatController() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        clients.put(session.getId(), new ChatClient(session, userDetailsService));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ChatClient client = clients.remove(session.getId());
        if (client != null) {
            ChatRoom room = client.getRoom();
            if (room != null) {
                room.removeClient(client);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatClient chatClient = clients.get(session.getId());
        try {
            if (chatClient.getRoom() != null) {
                ChatRoom room = chatClient.getRoom();
                ClientMessage clientMessage = objectMapper.readValue(message.getPayload(), ClientMessage.class);
                processMessage(chatClient, room, clientMessage);
            } else {
                ClientHandshake handshake = objectMapper.readValue(message.getPayload(), ClientHandshake.class);
                if (!connectUser(chatClient, handshake)) {
                    session.close();
                }
            }
        } catch (Exception e) {
            session.close();
            throw e;
        }
    }

    private void processMessage(ChatClient chatClient, ChatRoom room, ClientMessage clientMessage) {
        boolean isAdmin = chatClient.getUser().getGroups().contains(userService.getGroupByName("Admin")) && room.getName().equals("admin");
        switch (clientMessage.getType()) {
            case MESSAGE:
                if (isAdmin) {
                    broadcastMessage(chatClient, clientMessage);
                } else {
                    room.addMessage(chatClient, clientMessage, false);
                }
                break;
            case HISTORY:
                room.sendHistory(chatClient);
                break;
            case UPDATES:
                String[] parts = clientMessage.getMessage().split("-");
                int fromId = Integer.parseInt(parts[0]);
                int toId = Integer.parseInt(parts[1]);
                room.sendUpdates(chatClient, fromId, toId);
                break;
            case TYPOING:
                room.makeTypoer(chatClient);
                break;
            case PREVIEW:
                int postid = Integer.parseInt(clientMessage.getMessage());
                ChatMessage chatMessage = sparseMessages.get(postid);
                if (chatMessage == null) {
                    Message message = messageService.loadById(postid);
                    if (message != null) {
                        chatMessage = new ChatMessage(message);
                        sparseMessages.put(postid, chatMessage);
                    }
                }
                if (chatMessage != null) {
                    room.deliverTextMessage(chatClient, room.makeTextMessage(MessageType.PREVIEW, chatMessage));
                }
                break;
        }
    }

    private void broadcastMessage(ChatClient chatClient, ClientMessage clientMessage) {
        for (ChatRoom room : rooms.values()) {
            room.addMessage(chatClient, clientMessage, true);
        }
    }

    private boolean connectUser(ChatClient chatClient, ClientHandshake handshake) {
        Stream stream = streamService.getStream(handshake.getApp(), handshake.getStream());
        String appStream = handshake.getApp() + '/' + handshake.getStream();
        boolean isAdmin = chatClient.getUser().getGroups().contains(userService.getGroupByName("Admin")) && handshake.getStream().equals("admin");
        if (stream == null && !isAdmin) {
            return false;
        }

        ChatRoom room = rooms.get(appStream);
        if (room == null) {
            room = new ChatRoom(messageService, objectMapper, handshake.getApp(), handshake.getStream());
            if (!isAdmin) {
                stream.setChatRoom(room);
            }
            rooms.put(appStream, room);
        }
        room.addClient(chatClient, handshake);

        return true;
    }

    public ChatRoom getRoom(String appStream) {
        return rooms.get(appStream);
    }
}
