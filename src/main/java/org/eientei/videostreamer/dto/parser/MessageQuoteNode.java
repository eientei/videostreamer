package org.eientei.videostreamer.dto.parser;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-14
 * Time: 23:40
 */
public class MessageQuoteNode extends MessageContainerNode {
    int level;

    public MessageQuoteNode() {
        super(MessageNodeType.QUOTE);
    }

    @Override
    public boolean add(String text) {
        this.level = text.length();
        return true;
    }

    public int getLevel() {
        return level;
    }
}
