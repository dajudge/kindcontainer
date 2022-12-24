package com.dajudge.kindcontainer.client.model.admission.v1;

import java.util.ArrayList;
import java.util.List;

public class RuleWithOperations {
    private List<String> apiGroups = new ArrayList<String>();
    private List<String> apiVersions = new ArrayList<String>();
    private List<String> operations = new ArrayList<String>();
    private List<String> resources = new ArrayList<String>();
    private String scope;

    public List<String> getApiGroups() {
        return apiGroups;
    }

    public void setApiGroups(final List<String> apiGroups) {
        this.apiGroups = apiGroups;
    }

    public List<String> getApiVersions() {
        return apiVersions;
    }

    public void setApiVersions(final List<String> apiVersions) {
        this.apiVersions = apiVersions;
    }

    public List<String> getOperations() {
        return operations;
    }

    public void setOperations(final List<String> operations) {
        this.operations = operations;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(final List<String> resources) {
        this.resources = resources;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }
}
