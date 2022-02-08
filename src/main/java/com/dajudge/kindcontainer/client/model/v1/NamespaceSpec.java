package com.dajudge.kindcontainer.client.model.v1;

import java.util.List;

public class NamespaceSpec {
    private List<String> finalizers;

    public List<String> getFinalizers() {
        return finalizers;
    }

    public void setFinalizers(List<String> finalizers) {
        this.finalizers = finalizers;
    }

    @Override
    public String toString() {
        return "NamespaceSpec{" +
                "finalizers=" + finalizers +
                '}';
    }
}
