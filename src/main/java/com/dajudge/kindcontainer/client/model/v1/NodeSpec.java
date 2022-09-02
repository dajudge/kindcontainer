package com.dajudge.kindcontainer.client.model.v1;

import java.util.List;

public class NodeSpec {
    private List<Taint> taints;

    public List<Taint> getTaints() {
        return taints;
    }

    public void setTaints(final List<Taint> taints) {
        this.taints = taints;
    }
}
