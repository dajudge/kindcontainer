package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.client.model.base.LabelSelector;
import com.dajudge.kindcontainer.client.model.base.LabelSelectorRequirement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class LabelSelectorBuilderImpl<P> implements LabelSelectorBuilder<P> {

    private final P parent;
    private final Map<String, String> matchLabels = new HashMap<>();
    private final List<LabelSelectorRequirementBuilderImpl<?>> matchExpressions = new ArrayList<>();

    public LabelSelectorBuilderImpl(final P parent) {
        this.parent = parent;
    }

    @Override
    public LabelSelectorBuilderImpl<P> addMatchLabel(final String key, final String value) {
        matchLabels.put(key, value);
        return this;
    }

    @Override
    public LabelSelectorBuilderImpl<P> addMatchLabels(final Map<String, String> matchLabels) {
        matchLabels.putAll(matchLabels);
        return this;
    }

    @Override
    public LabelSelectorRequirementBuilderImpl<LabelSelectorBuilder<P>> withNewMatchExpression() {
        return new LabelSelectorRequirementBuilderImpl<>(this);
    }

    public LabelSelector toLabelSelector() {
        final LabelSelector labelSelector = new LabelSelector();
        labelSelector.setMatchLabels(matchLabels.isEmpty() ? null : matchLabels);
        labelSelector.setMatchExpressions(matchExpressions.isEmpty() ? null : toLabelSelectorRequirement(matchExpressions));
        return labelSelector;
    }

    private List<LabelSelectorRequirement> toLabelSelectorRequirement(final List<LabelSelectorRequirementBuilderImpl<?>> matchExpressions) {
        return matchExpressions.stream()
                .map(LabelSelectorRequirementBuilderImpl::labelSelectorRequirement)
                .collect(Collectors.toList());
    }

    @Override
    public P endLabelSelector() {
        return parent;
    }
}
