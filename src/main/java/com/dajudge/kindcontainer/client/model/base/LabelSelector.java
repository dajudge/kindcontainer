package com.dajudge.kindcontainer.client.model.base;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LabelSelector {
    private List<LabelSelectorRequirement> matchExpressions = new ArrayList<>();
    private Map<String, String> matchLabels = new LinkedHashMap<>();

    public List<LabelSelectorRequirement> getMatchExpressions() {
        return matchExpressions;
    }

    public void setMatchExpressions(final List<LabelSelectorRequirement> matchExpressions) {
        this.matchExpressions = matchExpressions;
    }

    public Map<String, String> getMatchLabels() {
        return matchLabels;
    }

    public void setMatchLabels(final Map<String, String> matchLabels) {
        this.matchLabels = matchLabels;
    }
}
