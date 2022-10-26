package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.client.model.base.LabelSelectorRequirement;

import java.util.List;

import static java.util.Arrays.asList;

class LabelSelectorRequirementBuilderImpl<P> implements LabelSelectorRequirementBuilder<P> {
    private final P parent;
    private String key;
    private List<String> values;
    private String operator;

    public LabelSelectorRequirementBuilderImpl(final P parent) {
        this.parent = parent;
    }

    @Override
    public LabelSelectorRequirementBuilderImpl<P> withKey(final String key) {
        this.key = key;
        return this;
    }

    @Override
    public LabelSelectorRequirementBuilderImpl<P> withValues(final List<String> values) {
        this.values = values;
        return this;
    }

    @Override
    public LabelSelectorRequirementBuilder<P> withValues(final String... values) {
        return withValues(asList(values));
    }

    @Override
    public LabelSelectorRequirementBuilderImpl<P> withOperator(final String operator) {
        this.operator = operator;
        return this;
    }

    public LabelSelectorRequirement labelSelectorRequirement(){
        final LabelSelectorRequirement ret = new LabelSelectorRequirement();
        ret.setKey(key);
        ret.setValues(values);
        ret.setOperator(operator);
        return ret;
    }

    @Override
    public P endLabelSelectorRequirement() {
        return parent;
    }
}
