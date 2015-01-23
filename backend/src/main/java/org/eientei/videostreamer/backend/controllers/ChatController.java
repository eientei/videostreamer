package org.eientei.videostreamer.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.videostreamer.backend.chat.ChatClient;
import org.eientei.videostreamer.backend.chat.ChatRoom;
import org.eientei.videostreamer.backend.orm.entity.Message;
import org.eientei.videostreamer.backend.orm.entity.Stream;
import org.eientei.videostreamer.backend.orm.service.MessageService;
import org.eientei.videostreamer.backend.orm.service.StreamService;
import org.eientei.videostreamer.backend.pojo.chat.*;
import org.eientei.videostreamer.backend.security.AppUserDetails;
import org.eientei.videostreamer.backend.utils.VideostreamerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-20
 * Time: 10:29
 */
@RequestMapping("chatinfo")
@Controller
public class ChatController extends TextWebSocketHandler {
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Map<String, ChatClient> clients = new ConcurrentHashMap<String, ChatClient>();
    private Map<String, ChatRoom> rooms = new ConcurrentHashMap<String, ChatRoom>();
    private Map<String, ChatClient> sessionMap = new ConcurrentHashMap<String, ChatClient>();

    @Autowired
    private StreamService streamService;

    @Autowired
    private MessageService messageService;
    private ObjectMapper mapper = new ObjectMapper();

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

    @RequestMapping(value = "getpreview", method = RequestMethod.POST)
    @ResponseBody
    public Object getpreview(@RequestBody ClientChatPreview clientChatPreview) {
        Message message = messageService.loadById(clientChatPreview.getId());
        return VideostreamerUtils.buildMessageItem(message);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AppUserDetails appUserDetails = (AppUserDetails) session.getAttributes().get("principal");
        clients.put(session.getId(), new ChatClient(session, appUserDetails));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ChatClient client = clients.remove(session.getId());
        if (client.getRoom() != null) {
            client.getRoom().removeClient(client);
            String sessionId = (String) client.getSession().getAttributes().get("httpSession");
            sessionMap.remove(sessionId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatClient client = clients.get(session.getId());
        ClientChat clientMessage = mapper.readValue(message.getPayload(), ClientChat.class);
        switch (clientMessage.getType()) {
            case CONNECT:
                processConnect(client, clientMessage.adaptTo(ClientChatConnect.class));
                break;
            case MESSAGE:
                processMessage(client, clientMessage.adaptTo(ClientChatMessage.class));
                break;
            case HISTORY:
                processHistory(client, clientMessage.adaptTo(ClientChatHistory.class));
                break;
            case TYPING:
                processTyping(client);
                break;
            case TOPIC:
                processTopic(client, clientMessage.adaptTo(ClientChatTopic.class));
                break;
            case ONLINE:
                processOnline(client);
                break;
        }
    }

    private void processOnline(ChatClient client) {
        client.getRoom().sendOnline();
    }

    public void sendTopic(String app, String name) {
        String appName = app + '/' + name;
        ChatRoom room = rooms.get(appName);
        if (room != null) {
            room.sendTopic();
        }
    }

    private void processTopic(ChatClient client, ClientChatTopic topic) {
        Stream stream = client.getRoom().getStream();

        if (stream.getAuthor().getId() != client.getAppUserDetails().getDataUser().getId()) {
            return;
        }

        streamService.updateStreamTopic(stream, topic.getTopic());
        client.getRoom().sendTopic();
    }

    private void processTyping(ChatClient client) {
        client.getRoom().markTyper(client);
    }

    private void processHistory(ChatClient client, ClientChatHistory history) {
        client.getRoom().sendHistory(client, history.getRefpoint());
    }

    private void processMessage(ChatClient client, ClientChatMessage relay) {
        client.getRoom().relayMessage(client, relay.getText());
    }

    private void processConnect(ChatClient client, ClientChatConnect connect) throws IOException {
        Stream stream = streamService.getStream(connect.getApp(), connect.getName());
        if (stream == null) {
            client.getSession().close();
            return;
        }

        String appName = connect.getApp() + '/' + connect.getName();

        ChatRoom room = rooms.get(appName);
        if (room == null) {
            room = new ChatRoom(stream, streamService, messageService);
            rooms.put(appName, room);
        }
        String sessionId = (String) client.getSession().getAttributes().get("httpSession");
        sessionMap.put(sessionId, client);

        client.setRoom(room);
        room.addClient(client);
    }

    public void updateLogin(String sessionId, AppUserDetails appUserDetails) {
        ChatClient client = sessionMap.get(sessionId);
        if (client != null) {
            client.setAppUserDetails(appUserDetails);
            client.getRoom().sendOnline();
        }
    }

    public ChatRoom getRoom(String app, String name) {
        String appName = app + '/' + name;
        return rooms.get(appName);
    }
}
