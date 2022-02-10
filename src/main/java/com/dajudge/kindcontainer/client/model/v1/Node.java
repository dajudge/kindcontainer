package com.dajudge.kindcontainer.client.model.v1;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;

public class Node extends WithMetadata {

    private NodeStatus status;

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public static class ItemList extends ResourceList<Node> {
    }

    public static class StreamItem extends WatchStreamItem<Node> {
    }
}
