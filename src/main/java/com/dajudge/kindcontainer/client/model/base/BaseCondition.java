package com.dajudge.kindcontainer.client.model.base;

public class BaseCondition {
    private String type;
    private String status;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "BaseCondition{" +
                "type='" + type + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
