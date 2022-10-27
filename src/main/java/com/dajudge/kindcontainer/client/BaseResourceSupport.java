
package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import java.util.Optional;

import static java.lang.String.format;

public class BaseResourceSupport<T extends WithMetadata, L extends ResourceList<T>, I extends WatchStreamItem<T>>
        implements Watchable<T, L, I> {
    private final HttpSupport support;
    private final TypeReference<T> type;
    private final TypeReference<L> listType;
    private final TypeReference<I> itemType;
    private final String path;

    public BaseResourceSupport(
            final HttpSupport support,
            final TypeReference<T> type,
            final TypeReference<L> listType,
            final TypeReference<I> itemType,
            final String path
    ) {
        this.support = support;
        this.type = type;
        this.listType = listType;
        this.itemType = itemType;
        this.path = path;
    }

    public L list() {
        return support.syncGet(path, listType);
    }

    public T create(final T resource) {
        return support.syncPost(path, resource, type);
    }

    public Optional<T> find(final String name) {
        try {
            return Optional.of(support.syncGet(namedPath(name), type));
        } catch (final NotFoundException e) {
            return Optional.empty();
        }
    }

    private String namedPath(String name) {
        return format("%s/%s", path, name);
    }

    public T delete(final String name) {
        return support.syncDelete(namedPath(name), type);
    }

    @Override
    public Watch watch(final WatchCallback<T> callback) {
        return support.watch(path, listType, itemType, callback);
    }
}
