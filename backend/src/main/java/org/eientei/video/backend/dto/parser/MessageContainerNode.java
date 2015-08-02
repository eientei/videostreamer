package org.eientei.video.backend.dto.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-21
 * Time: 12:50
 */
public abstract class MessageContainerNode extends MessageNode {
    private List<MessageNode> children = new ArrayList<MessageNode>();

    public MessageContainerNode(MessageNodeType type) {
        super(type);
    }

    @Override
    public boolean add(MessageNode node) {
        children.add(node);
        return true;
    }

    @Override
    public boolean add(String text) {
        return true;
    }

    public List<MessageNode> getChildren() {
        return children;
    }
}
