package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

public class BaseNamespacedSupport<T extends WithMetadata, L extends ResourceList<T>, I extends WatchStreamItem<T>>
        implements Watchable<T, L, I> {
    private final HttpSupport support;
    private final TypeReference<T> type;
    private final TypeReference<L> listType;
    private final TypeReference<I> itemType;
    private final NamespacedPath path;

    public interface NamespacedPath {
        String getNamespacedPath(final String namespace);

        String getClusterPath();
    }

    public BaseNamespacedSupport(
            final HttpSupport support,
            final TypeReference<T> type,
            final TypeReference<L> listType,
            final TypeReference<I> itemType,
            final NamespacedPath path) {
        this.support = support;
        this.type = type;
        this.listType = listType;
        this.itemType = itemType;
        this.path = path;
    }

    public BaseResourceSupport<T, L, I> inNamespace(final String namespace) {
        return new BaseResourceSupport<>(support, type, listType, itemType, path.getNamespacedPath(namespace));
    }

    @Override
    public Watch watch(final WatchCallback<T> callback) {
        return support.watch(path.getClusterPath(), listType, itemType, callback);
    }
}
