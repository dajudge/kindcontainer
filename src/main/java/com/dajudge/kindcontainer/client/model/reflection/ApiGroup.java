package com.dajudge.kindcontainer.client.model.reflection;

public class ApiGroup {
    private ApiGroupVersion preferredVersion;

    public void setPreferredVersion(ApiGroupVersion preferredVersion) {
        this.preferredVersion = preferredVersion;
    }

    public ApiGroupVersion getPreferredVersion() {
        return preferredVersion;
    }
}
