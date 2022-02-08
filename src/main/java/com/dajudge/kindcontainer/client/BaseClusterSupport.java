package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

public class BaseClusterSupport<T extends WithMetadata, L extends ResourceList<T>, I extends WatchStreamItem<T>> extends BaseResourceSupport<T, L, I> {
    private final HttpSupport support;
    private final TypeReference<L> listType;
    private final TypeReference<I> itemType;
    private final String path;

    public BaseClusterSupport(
            final HttpSupport support,
            final TypeReference<T> type,
            final TypeReference<L> listType,
            final TypeReference<I> itemType,
            final String path
    ) {
        super(support, type, listType, itemType, path);
        this.support = support;
        this.listType = listType;
        this.itemType = itemType;
        this.path = path;
    }
}
