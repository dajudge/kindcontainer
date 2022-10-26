package com.dajudge.kindcontainer.webhook;

import java.util.Map;

public interface LabelSelectorBuilder<P> {

    LabelSelectorBuilderImpl<P> addMatchLabel(String key, String value);

    LabelSelectorBuilderImpl<P> addMatchLabels(Map<String, String> matchLabels);

    LabelSelectorRequirementBuilderImpl<LabelSelectorBuilder<P>> withNewMatchExpression();

    P endLabelSelector();
}
