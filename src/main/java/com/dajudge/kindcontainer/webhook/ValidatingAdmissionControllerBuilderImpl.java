package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.client.TinyK8sClient;
import com.dajudge.kindcontainer.client.model.admission.v1.ValidatingWebhookConfiguration;
import com.dajudge.kindcontainer.client.model.base.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

class ValidatingAdmissionControllerBuilderImpl implements ValidatingAdmissionControllerBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ValidatingAdmissionControllerBuilderImpl.class);
    private final AdmissionControllerManager manager;
    private final Consumer<Consumer<TinyK8sClient>> onContainerStarted;
    private final List<WebhookBuilderImpl<?>> webhooks = new ArrayList<>();

    public ValidatingAdmissionControllerBuilderImpl(
            final AdmissionControllerManager manager,
            final Consumer<Consumer<TinyK8sClient>> onContainerStarted
    ) {
        this.manager = manager;
        this.onContainerStarted = onContainerStarted;
    }

    @Override
    public WebhookBuilderImpl<ValidatingAdmissionControllerBuilderImpl> withNewWebhook(final String name) {
        final WebhookBuilderImpl<ValidatingAdmissionControllerBuilderImpl> builder = new WebhookBuilderImpl<>(name, this);
        webhooks.add(builder);
        return builder;
    }

    @Override
    public void build() {
        final String configName = UUID.randomUUID().toString();
        final ValidatingWebhookConfiguration config = new ValidatingWebhookConfiguration();
        config.setKind(ValidatingWebhookConfiguration.class.getSimpleName());
        config.setApiVersion("admissionregistration.k8s.io/v1");
        config.setMetadata(new Metadata());
        config.getMetadata().setName(configName);
        config.setWebhooks(webhooks.stream()
                .map(w -> w.toWebhook(manager, configName))
                .collect(toList()));
        onContainerStarted.accept(client -> {
            config.getWebhooks().forEach(w -> LOG.debug("Validating admission controller: {} {}", w.getName(), w.getClientConfig().getUrl()));
            client.admissionRegistration().v1().validatingWebhookConfigurations().create(config);
        });
    }

}
