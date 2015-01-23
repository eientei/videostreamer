package org.eientei.videostreamer.backend.pojo.chat;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-21
 * Time: 11:32
 */
public class ServerChatOnlineItem implements Comparable<ServerChatOnlineItem> {
    private String hash;
    private String name;
    private boolean owner;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    @Override
    public int compareTo(ServerChatOnlineItem o) {
        return hash.compareTo(o.getHash());
    }
}
