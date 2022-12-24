package com.dajudge.kindcontainer.client.model.base;

import java.util.ArrayList;
import java.util.List;

public class LabelSelectorRequirement {
    private String key;
    private String operator;
    private List<String> values = new ArrayList<>();

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(final String operator) {
        this.operator = operator;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(final List<String> values) {
        this.values = values;
    }
}
