package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.model.reflection.ApiVersions;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

public class ApiVersionsSupport {
    private HttpSupport support;

    public ApiVersionsSupport(final HttpSupport support) {
        this.support = support;
    }

    public ApiVersions get() {
        return support.syncGet("/api", new TypeReference<ApiVersions>() {
        });
    }
}
