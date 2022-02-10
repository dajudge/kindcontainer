package com.dajudge.kindcontainer.client.model.apps;

import com.dajudge.kindcontainer.client.HttpSupport;

public class Fluent {
    private final HttpSupport support;

    public Fluent(final HttpSupport support) {
        this.support = support;
    }

    public com.dajudge.kindcontainer.client.model.apps.v1.Fluent v1() {
        return new com.dajudge.kindcontainer.client.model.apps.v1.Fluent(support);
    }
}
