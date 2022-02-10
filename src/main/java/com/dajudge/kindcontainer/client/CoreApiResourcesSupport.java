package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.reflection.ApiResourceList;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import static java.lang.String.format;

public class CoreApiResourcesSupport {
    private HttpSupport support;
    private String groupVersion;

    public CoreApiResourcesSupport(final HttpSupport support, final String groupVersion) {
        this.support = support;
        this.groupVersion = groupVersion;
    }

    public ApiResourceList get() {
        return support.syncGet(format("/api/%s", groupVersion), new TypeReference<ApiResourceList>() {
        });
    }
}
