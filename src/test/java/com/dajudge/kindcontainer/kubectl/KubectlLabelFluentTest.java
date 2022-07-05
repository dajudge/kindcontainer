package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import io.fabric8.kubernetes.api.model.Namespace;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.HashMap;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.apiServerContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubectlLabelFluentTest {
    @TestFactory
    public Stream<DynamicTest> adds_all_labels() {
        return apiServerContainers(this::assertAddsAllLabels);
    }

    private void assertAddsAllLabels(final KubernetesTestPackage<? extends ApiServerContainer<?>> testPkg) {
        runWithK8s(configureContainer(testPkg.newContainer()), k8s -> runWithClient(k8s, client -> {
            final Namespace namespace = client.namespaces().withName("default").get();
            assertNotNull(namespace);
            new HashMap<String, String>() {{
                put("label1", "value1");
                put("label2", "value2");
            }}.forEach((k, v) -> {
                assertEquals(v, namespace.getMetadata().getLabels().get(k));
            });
        }));
    }

    private ApiServerContainer<?> configureContainer(final ApiServerContainer<?> container) {
        return container.withKubectl(kubectl -> kubectl.label
                .with("label1", "value1")
                .with(new HashMap<String, String>() {{
                    put("label2", "value2");
                }})
                .run("namespace", "default"));
    }

}
