package com.dajudge.kindcontainer.client.model.reflection;

import java.util.List;

public class ApiResource {
    private String group;
    private String kind;
    private String name;
    private Boolean namespaced;
    private List<String> verbs;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getNamespaced() {
        return namespaced;
    }

    public void setNamespaced(Boolean namespaced) {
        this.namespaced = namespaced;
    }

    public List<String> getVerbs() {
        return verbs;
    }

    public void setVerbs(List<String> verbs) {
        this.verbs = verbs;
    }
}
