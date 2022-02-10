package com.dajudge.kindcontainer.client.model.reflection;

import java.util.ArrayList;
import java.util.List;

public class ApiResourceList {
    private List<ApiResource> resources = new ArrayList<>();

    public List<ApiResource> getResources() {
        return resources;
    }

    public void setResources(List<ApiResource> resources) {
        this.resources = resources;
    }
}
