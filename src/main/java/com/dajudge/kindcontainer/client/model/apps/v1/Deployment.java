package com.dajudge.kindcontainer.client.model.apps.v1;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;

public class Deployment extends WithMetadata {

    private DeploymentStatus status;

    public DeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(DeploymentStatus status) {
        this.status = status;
    }

    public static class ItemList extends ResourceList<Deployment> {}
    public static class StreamItem extends WatchStreamItem<Deployment> {}
}
