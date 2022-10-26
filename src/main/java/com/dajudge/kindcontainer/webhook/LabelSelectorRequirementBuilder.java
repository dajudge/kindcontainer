package com.dajudge.kindcontainer.webhook;

import java.util.List;

public interface LabelSelectorRequirementBuilder<P> {

    LabelSelectorRequirementBuilder<P> withKey(String key);

    LabelSelectorRequirementBuilder<P> withValues(List<String> values);

    LabelSelectorRequirementBuilder<P> withValues(String... values);

    LabelSelectorRequirementBuilder<P> withOperator(String operator);

    P endLabelSelectorRequirement();
}
