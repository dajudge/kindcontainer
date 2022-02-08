package com.dajudge.kindcontainer.client.model.reflection;

import com.dajudge.kindcontainer.client.*;

public class Fluent {
    private final HttpSupport support;

    public Fluent(final HttpSupport support) {
        this.support = support;
    }

    public ApiVersionsSupport apiVersions() {
        return new ApiVersionsSupport(support);
    }

    public CoreApiResourcesSupport coreApiResources(final String groupVersion) {
        return new CoreApiResourcesSupport(support, groupVersion);
    }

    public ApiGroupsSupport apiGroups() {
        return new ApiGroupsSupport(support);
    }

    public ApiResourcesSupport apiResources(final String groupVersion) {
        return new ApiResourcesSupport(support, groupVersion);
    }
}
