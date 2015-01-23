package org.eientei.videostreamer.backend.pojo.stream;

import org.eientei.videostreamer.backend.pojo.chat.ServerChatOnlineItem;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-22
 * Time: 15:11
 */
public class StreamIndexItem {
    private String app;
    private String name;
    private String topic;
    private String authorName;
    private String authorHash;
    private long since;
    private List<ServerChatOnlineItem> onlines = new ArrayList<ServerChatOnlineItem>();

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

    public String getAuthorHash() {
        return authorHash;
    }

    public void setAuthorHash(String authorHash) {
        this.authorHash = authorHash;
    }

    public long getSince() {
        return since;
    }

    public void setSince(long since) {
        this.since = since;
    }

    public List<ServerChatOnlineItem> getOnlines() {
        return onlines;
    }

    public void setOnlines(List<ServerChatOnlineItem> onlines) {
        this.onlines = onlines;
    }
}
