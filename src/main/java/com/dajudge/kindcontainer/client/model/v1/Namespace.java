package com.dajudge.kindcontainer.client.model.v1;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;

public class Namespace extends WithMetadata  {
    private NamespaceSpec spec;

    public NamespaceSpec getSpec() {
        return spec;
    }

    public void setSpec(NamespaceSpec spec) {
        this.spec = spec;
    }

    public static class ItemList extends ResourceList<Namespace> {}
    public static class StreamItem extends WatchStreamItem<Namespace> {}

    @Override
    public String toString() {
        return "Namespace{" +
                "spec=" + spec +
                "} " + super.toString();
    }
}
