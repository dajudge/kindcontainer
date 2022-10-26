package com.dajudge.kindcontainer.client.model.admission;

import com.dajudge.kindcontainer.client.HttpSupport;

public class Fluent {
    private final HttpSupport support;

    public Fluent(final HttpSupport support) {
        this.support = support;
    }

    public com.dajudge.kindcontainer.client.model.admission.v1.Fluent v1() {
        return new com.dajudge.kindcontainer.client.model.admission.v1.Fluent(support);
    }
}
