package org.eientei.video.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.video.orm.entity.Message;
import org.eientei.video.orm.entity.Stream;
import org.eientei.video.orm.service.MessageService;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-18
 * Time: 18:33
 */
public class ChatRoom {
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Set<ChatClient> clients = new ConcurrentSkipListSet<ChatClient>();
    private Map<ChatClient, Long> typingClients = new ConcurrentHashMap<ChatClient, Long>();

    private List<ChatMessage> before = new CopyOnWriteArrayList<ChatMessage>();
    private List<ChatMessage> after = new CopyOnWriteArrayList<ChatMessage>();

    private boolean allMessagesLoaded = false;
    private MessageService messageService;
    private ObjectMapper objectMapper;
    private String app;
    private String stream;

    private static final int BULK_SIZE = 30;

    public class DescendingIterator {
        private int afterPos;
        private int beforePos;

        private int limitId;
        private ChatMessage nextElem;
        private boolean advanced = true;


        public DescendingIterator(int afterPos, int beforePos, int limitId) {
            this.afterPos = afterPos;
            this.beforePos = beforePos;
            this.limitId = limitId;
        }

        private boolean hasNextTest() {
            return (afterPos >= 0) || (beforePos < before.size());
        }

        public boolean hasNext() {
            if (!hasNextTest() && !allMessagesLoaded) {
                loadOlderMessages();
            }

            if (hasNextTest() && advanced) {
                nextElem = actualNext();
                advanced = false;
            }

            return hasNextTest() && (nextElem != null && nextElem.getId() > limitId);
        }

        private ChatMessage actualNext() {
            ChatMessage message = null;

            if (afterPos >= 0 && afterPos < after.size()) {
                message = after.get(afterPos);
            } else if (beforePos < before.size()) {
                message = before.get(beforePos);
            }

            return message;
        }

        public ChatMessage next() {
            if (nextElem == null) {
                throw new NoSuchElementException("No next element");
            }
            if (afterPos >= 0 && afterPos < after.size()) {
                afterPos--;
            } else if (beforePos < before.size()) {
                beforePos++;
            }
            advanced = true;
            return nextElem;
        }
    }

    public ChatRoom(MessageService messageService, ObjectMapper objectMapper, String app, String stream) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.app = app;
        this.stream = stream;

        if (app == null || stream == null) {
            throw new NullPointerException("Null app or stream");
        }

        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                refreshTypoers();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        loadOlderMessages();
    }

    private void refreshTypoers() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ChatClient, Long>> iter = typingClients.entrySet().iterator();
        boolean updated = false;
        while (iter.hasNext()) {
            Map.Entry<ChatClient, Long> pair = iter.next();
            if ((now - pair.getValue()) > 1000) {
                iter.remove();
                updated = true;
            }
        }
        if (updated) {
            deliverTypoingUpdate();
        }
    }

    private void deliverTypoingUpdate() {
        TextMessage message = makeTypoingMessage();
        if (message != null) {
            for (ChatClient client : clients) {
                deliverTextMessage(client, message);
            }
        }
    }

    public void sendHistory(ChatClient client) {
        DescendingIterator iterator = client.getMessageIterator();
        if (iterator != null) {
            ChatMessageList messageList = new ChatMessageList();
            int i = BULK_SIZE;
            while (iterator.hasNext() && i > 0) {
                messageList.getChatMessages().add(iterator.next());
                i--;
            }
            messageList.setHasMore(iterator.hasNext());
            if (messageList.getChatMessages().isEmpty()) {
                client.setMessageIterator(null);
            }
            deliverTextMessage(client, makeTextMessage(MessageType.HISTORY, messageList));
        }
    }

    public void sendUpdates(ChatClient client, int fromId, int toId) {
        DescendingIterator iterator = makeIterator(fromId, toId);
        if (iterator != null) {
            ChatMessageList messageList = new ChatMessageList();
            int i = BULK_SIZE;
            while (iterator.hasNext() && i > 0) {
                messageList.getChatMessages().add(iterator.next());
                i--;
            }
            messageList.setHasMore(iterator.hasNext());
            if (!messageList.getChatMessages().isEmpty()) {
                deliverTextMessage(client, makeTextMessage(MessageType.UPDATES, messageList));
            }
        }
    }

    private TextMessage makeTypoingMessage() {
        Set<String> hashes = new HashSet<String>();
        for (ChatClient client : typingClients.keySet()) {
            hashes.add(client.getUserHash());
        }
        return makeTextMessage(MessageType.TYPOING, hashes);
    }

    public TextMessage makeTextMessage(MessageType type, Object data) {
        try {
            return new TextMessage(objectMapper.writeValueAsString(new ServerMessage(type, data)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deliverTextMessage(ChatClient chatClient, TextMessage textMessage) {
        try {
            if (chatClient.getSession().isOpen()) {
                chatClient.getSession().sendMessage(textMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadOlderMessages() {
        int id = 0;
        if (!before.isEmpty()) {
            id = before.get(before.size() - 1).getId();
        }
        List<Message> messages  = messageService.loadLastBackMessages(BULK_SIZE, id, app, stream);

        if (messages.isEmpty()) {
            allMessagesLoaded = true;
        }

        for (Message message : messages) {
            before.add(new ChatMessage(message));
        }
    }

    public void makeTypoer(ChatClient client) {
        boolean was = typingClients.containsKey(client);
        typingClients.put(client, System.currentTimeMillis());

        if (!was) {
            deliverTypoingUpdate();
        }
    }

    private void deliverOnlinesUpdate() {
        Set<String> hashes = new HashSet<String>();
        for (ChatClient client : clients) {
            hashes.add(client.getUserHash());
        }
        TextMessage onlinesMessage = makeTextMessage(MessageType.ONLINES, hashes);
        for (ChatClient c : clients) {
            deliverTextMessage(c, onlinesMessage);
        }
    }

    private DescendingIterator makeIterator(int fromId, int toId) {
        int afterPos = -1;
        int beforePos = 0;

        if (fromId < toId) {
            return null;
        }

        if (!after.isEmpty() && fromId >= after.get(0).getId()) {
            for (int i = 0; i < after.size(); i++) {
                if (after.get(i).getId() >= fromId) {
                    if (after.get(i).getId() == fromId && i > 0) {
                        i--;
                    }
                    afterPos = i;
                    break;
                }
            }
        } else {
            int maxSize = before.size();
            while (!allMessagesLoaded && fromId < before.get(maxSize - 1).getId()) {
                loadOlderMessages();
                maxSize = before.size();
            }
            if (fromId >= before.get(maxSize - 1).getId()) {
                int i = maxSize - 1;
                while (i >= 0) {
                    if (before.get(i).getId() >= fromId) {
                        if (before.get(i).getId() == fromId) {
                            i++;
                        }
                        break;
                    }
                    i--;
                }
                beforePos = i;
            }
        }
        return new DescendingIterator(afterPos, beforePos, toId);
    }

    public synchronized void addClient(ChatClient chatClient, ClientHandshake handshake) {
        chatClient.setRoom(this);
        clients.add(chatClient);

        TextMessage successMessage = makeTextMessage(MessageType.SUCCESS, chatClient.getUserHash());
        deliverTextMessage(chatClient, successMessage);

        if (handshake.isOldUser() && !before.isEmpty()) {
            int newId = handshake.getNewMessageId();
            int oldId = handshake.getOldMessageId();

            if (newId > 0) {
                ChatMessage newestMessage = after.isEmpty() ? before.get(0) : after.get(after.size() - 1);
                if (newId < newestMessage.getId()) {
                    sendUpdates(chatClient, newestMessage.getId(), newId);
                }
            }
            if (oldId > 0) {
                DescendingIterator messageIterator = makeIterator(oldId, -1);
                chatClient.setMessageIterator(messageIterator);
            }
        } else {
            DescendingIterator messageIterator = new DescendingIterator(after.size() - 1, 0, -1);
            chatClient.setMessageIterator(messageIterator);
            sendHistory(chatClient);
        }

        deliverOnlinesUpdate();

        TextMessage typoingMessage = makeTypoingMessage();
        if (typoingMessage != null) {
            deliverTextMessage(chatClient, typoingMessage);
        }
    }

    public void addMessage(ChatClient client, ClientMessage clientMessage) {
        Message message = new Message();
        message.setApp(app);
        message.setName(stream);
        message.setMessage(clientMessage.getMessage());
        message.setRemote(client.getRemote());
        message.setAuthor(client.getUser());

        messageService.storeMessage(message);

        ChatMessage hashedMessage = new ChatMessage(message);
        after.add(hashedMessage);

        TextMessage textMessage = makeTextMessage(MessageType.MESSAGE, hashedMessage);

        for (ChatClient c : clients) {
            deliverTextMessage(c, textMessage);
        }
    }

    public synchronized void removeClient(ChatClient client) {
        clients.remove(client);
        deliverOnlinesUpdate();
    }

    public int getClients() {
        return clients.size();
    }
}
