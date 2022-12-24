package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.client.model.admission.v1.RuleWithOperations;

import java.util.List;

import static java.util.Arrays.asList;

class WebhookRuleBuilderImpl<R> implements WebhookRuleBuilder<R> {
    private final R parent;
    private List<String> apiGroups;
    private List<String> apiVersions;
    private List<String> operations;
    private List<String> resources;
    private String scope;

    public WebhookRuleBuilderImpl(final R parent) {
        this.parent = parent;
    }

    @Override
    public WebhookRuleBuilderImpl<R> withApiGroups(final String... apiGroups) {
        this.apiGroups = asList(apiGroups);
        return this;
    }

    @Override
    public WebhookRuleBuilderImpl<R> withApiVersions(final String... apiVersions) {
        this.apiVersions = asList(apiVersions);
        return this;
    }


    @Override
    public WebhookRuleBuilderImpl<R> withOperations(final String... operations) {
        this.operations = asList(operations);
        return this;
    }


    @Override
    public WebhookRuleBuilderImpl<R> withResources(final String... resources) {
        this.resources = asList(resources);
        return this;
    }


    @Override
    public WebhookRuleBuilderImpl<R> withScope(final String scope) {
        this.scope = scope;
        return this;
    }

    @Override
    public R endRule() {
        return parent;
    }

    public RuleWithOperations toRuleWithOperations() {
        final RuleWithOperations rule = new RuleWithOperations();
        rule.setApiGroups(apiGroups);
        rule.setApiVersions(apiVersions);
        rule.setOperations(operations);
        rule.setResources(resources);
        rule.setScope(scope);
        return rule;
    }
}
