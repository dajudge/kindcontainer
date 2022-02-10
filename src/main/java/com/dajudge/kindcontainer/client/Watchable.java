package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;

public interface Watchable<T extends WithMetadata, L extends ResourceList<T>, I extends WatchStreamItem<T>> {
    Watch watch(WatchCallback<T> callback);
}
