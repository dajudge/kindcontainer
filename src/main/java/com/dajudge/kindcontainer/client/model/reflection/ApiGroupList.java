package com.dajudge.kindcontainer.client.model.reflection;

import java.util.ArrayList;
import java.util.List;

public class ApiGroupList {
    private List<ApiGroup> groups = new ArrayList<ApiGroup>();

    public List<ApiGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<ApiGroup> groups) {
        this.groups = groups;
    }
}
