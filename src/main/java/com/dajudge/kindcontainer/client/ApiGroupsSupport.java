package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.reflection.ApiGroupList;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

public class ApiGroupsSupport {
    private final HttpSupport support;

    public ApiGroupsSupport(final HttpSupport support) {
        this.support = support;
    }

    public ApiGroupList get() {
        return support.syncGet("/apis", new TypeReference<ApiGroupList>() {
        });
    }
}
