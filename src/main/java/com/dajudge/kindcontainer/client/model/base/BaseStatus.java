package com.dajudge.kindcontainer.client.model.base;

import java.util.List;

public class BaseStatus<C extends BaseCondition> {
    private List<C> conditions;

    public List<C> getConditions() {
        return conditions;
    }

    public void setConditions(List<C> conditions) {
        this.conditions = conditions;
    }

    @Override
    public String toString() {
        return "BaseStatus{" +
                "conditions=" + conditions +
                '}';
    }
}
