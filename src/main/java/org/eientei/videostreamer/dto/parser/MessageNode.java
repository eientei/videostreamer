package org.eientei.videostreamer.dto.parser;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-14
 * Time: 23:33
 */
public abstract class MessageNode {
    private MessageNodeType type;

    public MessageNode(MessageNodeType type) {
        this.type = type;
    }

    public MessageNode commitTo(MessageNode parent) {
        parent.add(this);
        return parent;
    }

    public abstract boolean add(MessageNode node);
    public abstract boolean add(String text);

    public static MessageNode make(Class<? extends MessageNode> node) {
        try {
            return node.newInstance();
        } catch (Exception ignore) {
        }
        return null;
    }

    public MessageNodeType getType() {
        return type;
    }
}
