package com.dajudge.kindcontainer.client.model.base;

import java.util.List;

public class ResourceList<T extends WithMetadata> {
    private ResourceListMetadata metadata;
    private List<T> items;

    public ResourceListMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ResourceListMetadata metadata) {
        this.metadata = metadata;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "KubernetesResourceList{" +
                "items=" + items +
                '}';
    }
}
