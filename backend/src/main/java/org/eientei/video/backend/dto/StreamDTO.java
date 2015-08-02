package org.eientei.video.backend.dto;

import org.eientei.video.backend.controller.model.ChatClient;
import org.eientei.video.backend.controller.model.ChatRoom;
import org.eientei.video.backend.orm.entity.Stream;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 13:28
 */
public class StreamDTO {
    private String app;
    private String name;
    private String topic;
    private String authorName;
    private String authorHashicon;
    private String image;
    private long since;
    protected boolean ownstream = false;
    private Set<ChatClientDTO> online = new TreeSet<ChatClientDTO>(new Comparator<ChatClientDTO>() {
        @Override
        public int compare(ChatClientDTO o1, ChatClientDTO o2) {
            return o1.getHash().compareTo(o2.getHash());
        }
    });

    public StreamDTO(Stream stream, ChatRoom room) {
        long time = System.currentTimeMillis();
        if (stream != null) {
            this.app = stream.getApp();
            this.name = stream.getName();
            this.topic = stream.getTopic();
            this.authorName = stream.getAuthor().getName();
            this.authorHashicon = Util.hash(stream.getAuthor());
            this.image = stream.getIdleImage();
            this.since = 0;
            if (stream.getSince() != null) {
                this.since = time - stream.getSince().getTime();
            }
            if (room != null) {
                for (ChatClient client : room.getOnline()) {
                    if (!client.getUser().getUsername().equals(stream.getAuthor().getName())) {
                        online.add(new ChatClientDTO(client, time));
                    }
                }
            }
        }
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorHashicon() {
        return authorHashicon;
    }

    public void setAuthorHashicon(String authorHashicon) {
        this.authorHashicon = authorHashicon;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public long getSince() {
        return since;
    }

    public void setSince(long since) {
        this.since = since;
    }

    public Set<ChatClientDTO> getOnline() {
        return online;
    }

    public void setOnline(Set<ChatClientDTO> online) {
        this.online = online;
    }

    public boolean isOwnstream() {
        return ownstream;
    }

    public void setOwnstream(boolean ownstream) {
        this.ownstream = ownstream;
    }
}
