package com.dajudge.kindcontainer.webhook;

import java.util.Map;

public interface LabelSelectorBuilder<P> {

    LabelSelectorBuilder<P> addMatchLabel(String key, String value);

    LabelSelectorBuilder<P> addMatchLabels(Map<String, String> matchLabels);

    LabelSelectorRequirementBuilder<LabelSelectorBuilder<P>> withNewMatchExpression();

    P endLabelSelector();
}
