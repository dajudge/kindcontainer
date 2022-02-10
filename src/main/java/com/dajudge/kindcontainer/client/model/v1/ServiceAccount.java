package com.dajudge.kindcontainer.client.model.v1;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;

import java.util.List;

public class ServiceAccount extends WithMetadata {

    private List<ObjectReference> secrets;

    public List<ObjectReference> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<ObjectReference> secrets) {
        this.secrets = secrets;
    }

    public static class ItemList extends ResourceList<ServiceAccount> {
    }

    public static class StreamItem extends WatchStreamItem<ServiceAccount> {
    }
}
