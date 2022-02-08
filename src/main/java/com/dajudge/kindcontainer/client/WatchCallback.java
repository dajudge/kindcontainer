package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.base.ResourceAction;

public interface WatchCallback<T> {
    void onAction(final ResourceAction action, final T resource);

    void onClose();
}
