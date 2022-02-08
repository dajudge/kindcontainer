package com.dajudge.kindcontainer.client.model.v1;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;

import java.util.Map;

public class Secret extends WithMetadata {

    private String type;
    private Map<String, String> data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public static class ItemList extends ResourceList<Secret> {
    }

    public static class StreamItem extends WatchStreamItem<Secret> {
    }

}
