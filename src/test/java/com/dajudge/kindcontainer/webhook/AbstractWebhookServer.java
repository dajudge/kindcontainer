package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.client.http.Response;
import com.dajudge.kindcontainer.client.http.TinyHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReviewBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public abstract class AbstractWebhookServer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractWebhookServer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String path;
    private Service service;
    private int port;

    protected AbstractWebhookServer(String path) {
        this.path = path;
    }

    protected abstract AdmissionReview review(final AdmissionReview review);

    public AbstractWebhookServer start() {
        LOG.info("Starting webhook server: {}", getClass().getSimpleName());
        service = Service.ignite().port(0);
        service.post(path, (req, res) -> {
            final AdmissionReview review = OBJECT_MAPPER.readValue(req.body(), AdmissionReview.class);
            final HasMetadata object = (HasMetadata) review.getRequest().getObject();
            if(!isTestResource(object)) {
                if(LOG.isDebugEnabled()) {
                    LOG.info("Ignoring admission review: {} {}", getClass().getSimpleName(), object.getMetadata());
                }else{
                    LOG.info("Ignoring admission review: {} {}", getClass().getSimpleName(), displayName(object));
                }
                return new AdmissionReviewBuilder()
                        .withNewResponse()
                        .withUid(review.getRequest().getUid())
                        .withAllowed(true)
                        .endResponse()
                        .build();
            }
            if(LOG.isDebugEnabled()) {
                LOG.info("Handling admission review: {} {}", getClass().getSimpleName(), review);
            }else{
                LOG.info("Handling admission review: {} {}", getClass().getSimpleName(), displayName(object));
            }
            return OBJECT_MAPPER.writeValueAsString(review(review));
        });
        service.init();
        service.awaitInitialization();
        port = service.port();

        final String url = String.format("http://localhost:%s", port);
        LOG.info("Awaiting readiness of webhook server: {} at {}", getClass().getSimpleName(), url);
        final TinyHttpClient http = TinyHttpClient.newHttpClient().build();
        await().ignoreExceptions().untilAsserted(() -> {
            try (final Response result = http.request()
                    .method("GET", null)
                    .url(url)
                    .execute()) {
                assertEquals(404, result.code());
            }
        });

        LOG.info("Started webhook server: {} at port {}", getClass().getSimpleName(), port);

        return this;
    }

    private static String displayName(HasMetadata object) {
        return String.format("%s/%s", object.getMetadata().getNamespace(), object.getMetadata().getName());
    }

    private static boolean isTestResource(final HasMetadata cm) {
        return Optional.of(cm.getMetadata().getLabels()).map(it -> it.get("testresource")).orElse("false").equals("true");
    }

    @Override
    public void close() {
        LOG.info("Stopping webhook server: {}", getClass().getSimpleName());
        service.stop();
    }

    protected int getPort() {
        return port;
    }

    public abstract void register(final AdmissionControllerBuilder admission);
}
