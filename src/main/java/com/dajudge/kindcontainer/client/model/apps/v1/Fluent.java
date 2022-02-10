package com.dajudge.kindcontainer.client.model.apps.v1;

import com.dajudge.kindcontainer.client.BaseNamespacedSupport;
import com.dajudge.kindcontainer.client.BaseResourceSupport;
import com.dajudge.kindcontainer.client.HttpSupport;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import static java.lang.String.format;

public class Fluent {
    private final HttpSupport support;

    public Fluent(HttpSupport support) {
        this.support = support;
    }

    public BaseNamespacedSupport<Deployment, Deployment.ItemList, Deployment.StreamItem> deployments() {
        return new BaseNamespacedSupport<>(
                support,
                new TypeReference<Deployment>() {
                },
                new TypeReference<Deployment.ItemList>() {
                },
                new TypeReference<Deployment.StreamItem>() {
                },
                new BaseNamespacedSupport.NamespacedPath() {
                    @Override
                    public String getNamespacedPath(String namespace) {
                        return format("/apis/apps/v1/namespaces/%s/deployments", namespace);
                    }

                    @Override
                    public String getClusterPath() {
                        return "/apis/apps/v1/deployments";
                    }
                }
        );
    }
}
