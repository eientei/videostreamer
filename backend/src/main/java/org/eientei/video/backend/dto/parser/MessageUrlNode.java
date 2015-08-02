package org.eientei.video.backend.dto.parser;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-14
 * Time: 23:49
 */
public class MessageUrlNode extends MessageNode {
    private String url;

    public MessageUrlNode() {
        super(MessageNodeType.URL);
    }

    @Override
    public boolean add(MessageNode node) {
        return true;
    }

    @Override
    public boolean add(String text) {
        this.url = text;
        return true;
    }

    public String getUrl() {
        return url;
    }
}
