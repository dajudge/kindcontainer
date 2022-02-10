package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.reflection.ApiResourceList;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import static java.lang.String.format;

public class ApiResourcesSupport {
    private final HttpSupport support;
    private final String groupVersion;

    public ApiResourcesSupport(final HttpSupport support, final String groupVersion) {
        this.support = support;
        this.groupVersion = groupVersion;
    }

    public ApiResourceList get() {
        return support.syncGet(format("/apis/%s", groupVersion), new TypeReference<ApiResourceList>() {
        });
    }
}
