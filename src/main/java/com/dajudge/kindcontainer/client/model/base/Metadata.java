package com.dajudge.kindcontainer.client.model.base;

import java.util.HashMap;
import java.util.Map;

public class Metadata {
    private Map<String, String> annotations;
    private String namespace;
    private String name;
    private String resourceVersion;
    private String deletionTimestamp;

    public String getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setDeletionTimestamp(String deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "annotations=" + annotations +
                ", namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                ", resourceVersion='" + resourceVersion + '\'' +
                ", deletionTimestamp='" + deletionTimestamp + '\'' +
                '}';
    }
}
