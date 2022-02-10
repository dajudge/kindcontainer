package com.dajudge.kindcontainer.client.model.v1;

import com.dajudge.kindcontainer.client.BaseResourceSupport;
import com.dajudge.kindcontainer.client.HttpSupport;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import static java.lang.String.format;

public class NamespacesSupport extends BaseResourceSupport<Namespace, Namespace.ItemList, Namespace.StreamItem> {

    private final HttpSupport support;

    public NamespacesSupport(final HttpSupport support) {
        super(
                support,
                new TypeReference<Namespace>() {
                },
                new TypeReference<Namespace.ItemList>() {
                },
                new TypeReference<Namespace.StreamItem>() {
                },
                "/api/v1/namespaces"
        );
        this.support = support;
    }

    public Namespace finalizeNamespace(final Namespace namespace) {
        // https://cloud.redhat.com/blog/the-hidden-dangers-of-terminating-namespaces
        final String path = format("/api/v1/namespaces/%s/finalize", namespace.getMetadata().getName());
        return support.syncPut(path, namespace, new TypeReference<Namespace>() {
        });
    }
}
