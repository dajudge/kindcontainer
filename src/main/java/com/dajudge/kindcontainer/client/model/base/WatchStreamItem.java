package com.dajudge.kindcontainer.client.model.base;

public class WatchStreamItem<T extends WithMetadata> {
    private ResourceAction type;
    private T object;

    public ResourceAction getType() {
        return type;
    }

    public void setType(ResourceAction type) {
        this.type = type;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }
}
