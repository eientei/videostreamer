package org.eientei.videostreamer.dto.parser;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-14
 * Time: 23:37
 */
public class MessageTextNode extends MessageNode {
    private String text;

    public MessageTextNode() {
        super(MessageNodeType.TEXT);
    }

    @Override
    public boolean add(MessageNode node) {
        return true;
    }

    @Override
    public boolean add(String text) {
        if (this.text == null) {
            this.text = text;
        } else {
            this.text += " " + text;
        }
        return true;
    }

    public String getText() {
        return text;
    }
}
