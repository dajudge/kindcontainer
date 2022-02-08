package com.dajudge.kindcontainer.client.model.v1;

import com.dajudge.kindcontainer.client.BaseClusterSupport;
import com.dajudge.kindcontainer.client.BaseResourceSupport;
import com.dajudge.kindcontainer.client.HttpSupport;
import com.dajudge.kindcontainer.client.BaseNamespacedSupport;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import static java.lang.String.format;

public class Fluent {
    private final HttpSupport support;

    public Fluent(final HttpSupport support) {
        this.support = support;
    }

    public NamespacesSupport namespaces() {
        return new NamespacesSupport(support);
    }

    public BaseNamespacedSupport<ServiceAccount, ServiceAccount.ItemList, ServiceAccount.StreamItem> serviceAccounts() {
        return new BaseNamespacedSupport<>(
                support,
                new TypeReference<ServiceAccount>() {
                },
                new TypeReference<ServiceAccount.ItemList>() {
                },
                new TypeReference<ServiceAccount.StreamItem>() {
                },
                new BaseNamespacedSupport.NamespacedPath() {
                    @Override
                    public String getNamespacedPath(String namespace) {
                        return format("/api/v1/namespaces/%s/serviceaccounts", namespace);
                    }

                    @Override
                    public String getClusterPath() {
                        return "/api/v1/serviceaccounts";
                    }
                }
        );
    }

    public BaseClusterSupport<Node, Node.ItemList, Node.StreamItem> nodes() {
        return new BaseClusterSupport<>(
                support,
                new TypeReference<Node>() {
                },
                new TypeReference<Node.ItemList>() {
                },
                new TypeReference<Node.StreamItem>() {
                },
                "/api/v1/nodes"
        );
    }

    public BaseNamespacedSupport<Secret, Secret.ItemList, Secret.StreamItem> secrets() {
        return new BaseNamespacedSupport<>(
                support,
                new TypeReference<Secret>() {
                },
                new TypeReference<Secret.ItemList>() {
                },
                new TypeReference<Secret.StreamItem>() {
                },
                new BaseNamespacedSupport.NamespacedPath() {
                    @Override
                    public String getNamespacedPath(final String namespace) {
                        return format("/api/v1/namespaces/%s/secrets", namespace);
                    }

                    @Override
                    public String getClusterPath() {
                        return "/api/v1/secrets";
                    }
                }
        );
    }

}
