package com.dajudge.kindcontainer.webhook;

import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReviewBuilder;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MutatingWebhookServer extends AbstractWebhookServer {

    private static final String PATCH = "[{\"op\": \"add\", \"path\": \"/metadata/annotations\", \"value\": {\"mutated\": \"true\"}}]";
    private static final String WEBHOOK_PATH = "/mutate";

    public MutatingWebhookServer() {
        super(WEBHOOK_PATH);
    }

    @Override
    protected AdmissionReview review(final AdmissionReview review) {
        return new AdmissionReviewBuilder()
                .withNewResponse()
                .withUid(review.getRequest().getUid())
                .withAllowed(true)
                .withPatchType("JSONPatch")
                .withPatch(Base64.getEncoder().encodeToString(PATCH.getBytes(UTF_8)))
                .endResponse()
                .build();
    }

    @Override
    public void register(final AdmissionControllerBuilder admission) {
        admission.mutating()
                .withNewWebhook("mutating.kindcontainer.dajudge.com")
                .atPort(getPort())
                .withPath(WEBHOOK_PATH)
                .withNewNamespaceSelector()
                .addMatchLabel("mutate", "true")
                .endLabelSelector()
                .withNewObjectSelector()
                .addMatchLabel("mutate", "true")
                .endLabelSelector()
                .withNewRule()
                .withApiGroups("")
                .withApiVersions("v1")
                .withOperations("CREATE", "UPDATE")
                .withResources("configmaps")
                .withScope("Namespaced")
                .endRule()
                .endWebhook()
                .build();
    }
}
