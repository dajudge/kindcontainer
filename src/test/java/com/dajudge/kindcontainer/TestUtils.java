package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.Response;
import org.testcontainers.shaded.okhttp3.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;

final class TestUtils {
    private TestUtils() {
    }

    static String createNewNamespace(final KubernetesClient client) {
        final Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                .withName(UUID.randomUUID().toString().replaceAll("-", ""))
                .endMetadata()
                .build();
        client.namespaces().create(namespace);
        return namespace.getMetadata().getName();
    }

    static Pod createTestPod(final KubernetesClient client, final String namespace) {
        final Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("testpod")
                .withNamespace(namespace)
                .withLabels(new HashMap<String, String>() {{
                    put("app", "nginx");
                }})
                .endMetadata()
                .withNewSpec()
                .withContainers(new ContainerBuilder()
                        .withName("test")
                        .withImage("nginx")
                        .withPorts(new ContainerPortBuilder()
                                .withContainerPort(80)
                                .withProtocol("TCP")
                                .build())
                        .build())
                .endSpec()
                .build();
        client.pods().inNamespace(namespace).create(pod);
        return pod;
    }

    static Callable<Boolean> http(final String url) {
        return () -> {
            try {
                final OkHttpClient client = new OkHttpClient();
                final Request request = new Request.Builder().url(url).build();
                final Response response = client.newCall(request).execute();
                try (final ResponseBody body = response.body()) {
                    return response.code() == 200;
                }
            } catch (final IOException e) {
                return false;
            }
        };
    }

    static boolean isRunning(final KubernetesClient client, final HasMetadata pod) {
        return "Running".equals(client.pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName())
                .get().getStatus().getPhase());
    }
}
