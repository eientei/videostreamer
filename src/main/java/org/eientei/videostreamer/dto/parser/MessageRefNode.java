package org.eientei.videostreamer.dto.parser;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-14
 * Time: 23:48
 */
public class MessageRefNode extends MessageNode {
    private long ref;

    public MessageRefNode() {
        super(MessageNodeType.REF);
    }

    @Override
    public boolean add(MessageNode node) {
        return true;
    }

    @Override
    public boolean add(String text) {
        ref = Long.parseLong(text);
        return true;
    }

    public long getRef() {
        return ref;
    }
}
