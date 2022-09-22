package com.dajudge.kindcontainer.util;

import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.Utils.ThrowingConsumer;
import com.dajudge.kindcontainer.Utils.ThrowingFunction;
import com.dajudge.kindcontainer.client.http.Response;
import com.dajudge.kindcontainer.client.http.TinyHttpClient;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.testcontainers.shaded.com.google.common.io.Resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.awaitility.Awaitility.await;

public final class TestUtils {
    private static final Random RANDOM = new Random();

    private TestUtils() {
    }

    public static String createNewNamespace(final KubernetesClient client) {
        final String name = randomIdentifier();
        final Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .build();
        client.namespaces().create(namespace);
        await("ServiceAccount 'default' in namespace " + name)
                .ignoreExceptions()
                .timeout(1, MINUTES)
                .until(() -> client.serviceAccounts().inNamespace(namespace.getMetadata().getName()).withName("default").get(), Objects::nonNull);
        return namespace.getMetadata().getName();
    }

    public static String randomIdentifier() {
        final String alphabet = "abcdefghijklmnopqrstuvwxyz";
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()))
                + randomUUID().toString().replaceAll("-", "");
    }

    public static Pod createSimplePod(final KubernetesClient client, final String namespace) {
        final Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName(randomIdentifier())
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

    public static Callable<Boolean> http(final String url) {
        return () -> {
            try {
                final TinyHttpClient client = TinyHttpClient.newHttpClient().build();
                try (final Response response = client.request().url(url).execute();) {
                    return response.code() == 200;
                }
            } catch (final IOException e) {
                return false;
            }
        };
    }

    public static boolean isRunning(final KubernetesClient client, final HasMetadata pod) {
        return "Running".equals(client.pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName())
                .get().getStatus().getPhase());
    }

    public static String stringResource(final String s) {
        try (final InputStream is = TestUtils.class.getClassLoader().getResourceAsStream(s)) {
            return readString(is);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read string resource: " + s, e);
        }
    }

    public static String readString(final InputStream is) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) > 0) {
            bos.write(buffer, 0, read);
        }
        return new String(bos.toByteArray(), UTF_8);
    }

    public static <T extends KubernetesContainer<T>, O> O runWithClient(
            final KubernetesContainer<?> k8s,
            final ThrowingConsumer<DefaultKubernetesClient, Exception> consumer
    ) {
        return runWithClient(k8s, client -> {
            consumer.accept(client);
            return null;
        });
    }

    public static <O> O runWithClient(
            final KubernetesContainer<?> k8s,
            final ThrowingFunction<DefaultKubernetesClient, O, Exception> consumer
    ) {
        try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(k8s.getKubeconfig()))) {
            try {
                return consumer.apply(client);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static byte[] readResource(final String resourceName) {
        try {
            return Resources.toByteArray(Resources.getResource(resourceName));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourceName, e);
        }
    }
}
