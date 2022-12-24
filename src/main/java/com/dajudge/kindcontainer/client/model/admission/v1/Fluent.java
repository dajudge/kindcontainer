package com.dajudge.kindcontainer.client.model.admission.v1;

import com.dajudge.kindcontainer.client.BaseClusterSupport;
import com.dajudge.kindcontainer.client.HttpSupport;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

public class Fluent {
    private final HttpSupport support;

    public Fluent(final HttpSupport support) {
        this.support = support;
    }

    public BaseClusterSupport<ValidatingWebhookConfiguration, ValidatingWebhookConfiguration.ItemList, ValidatingWebhookConfiguration.StreamItem> validatingWebhookConfigurations() {
        return new BaseClusterSupport<>(
                support,
                new TypeReference<ValidatingWebhookConfiguration>() {
                },
                new TypeReference<ValidatingWebhookConfiguration.ItemList>() {
                },
                new TypeReference<ValidatingWebhookConfiguration.StreamItem>() {
                },
                "/apis/admissionregistration.k8s.io/v1/validatingwebhookconfigurations"
        );
    }

    public BaseClusterSupport<MutatingWebhookConfiguration, MutatingWebhookConfiguration.ItemList, MutatingWebhookConfiguration.StreamItem> mutatingWebhookConfigurations() {
        return new BaseClusterSupport<>(
                support,
                new TypeReference<MutatingWebhookConfiguration>() {
                },
                new TypeReference<MutatingWebhookConfiguration.ItemList>() {
                },
                new TypeReference<MutatingWebhookConfiguration.StreamItem>() {
                },
                "/apis/admissionregistration.k8s.io/v1/mutatingwebhookconfigurations"
        );
    }
}
