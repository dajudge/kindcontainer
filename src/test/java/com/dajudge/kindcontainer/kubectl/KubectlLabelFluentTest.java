package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.APISERVER_CONTAINER;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubectlLabelFluentTest {
    @ParameterizedTest
    @MethodSource(APISERVER_CONTAINER)
    public void adds_all_labels(final Supplier<ApiServerContainer<?>> factory) {
        runWithK8s(createContainer(factory), k8s -> runWithClient(k8s, this::assertAddsAllLabels));
    }

    private ApiServerContainer<?> createContainer(final Supplier<ApiServerContainer<?>> factory) {
        return factory.get().withKubectl(kubectl -> kubectl.label
                .with("label1", "value1")
                .with(new HashMap<String, String>() {{
                    put("label2", "value2");
                }})
                .run("namespace", "default"));
    }

    private void assertAddsAllLabels(final DefaultKubernetesClient client) {
        final Namespace namespace = client.namespaces().withName("default").get();
        assertNotNull(namespace);
        new HashMap<String, String>() {{
            put("label1", "value1");
            put("label2", "value2");
        }}.forEach((k, v) -> {
            assertEquals(v, namespace.getMetadata().getLabels().get(k));
        });
    }
}
