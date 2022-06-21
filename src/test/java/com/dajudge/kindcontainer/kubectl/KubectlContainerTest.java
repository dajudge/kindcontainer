package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.KubernetesContainer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.ALL_CONTAINERS;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubectlContainerTest {

    @ParameterizedTest
    @MethodSource(ALL_CONTAINERS)
    public void kubectl_works(final Supplier<KubernetesContainer<?>> factory) {
        runWithK8s(createK8s(factory), k8s -> runWithClient(k8s, client -> {
            assertNotNull(client.inNamespace("my-namespace").serviceAccounts().withName("my-service-account").get());
        }));
    }

    private KubernetesContainer<?> createK8s(final Supplier<KubernetesContainer<?>> k8sFactory) {
        return k8sFactory.get()
                .withKubectl(kubectl -> kubectl.apply.fileFromClasspath("manifests/serviceaccount1.yaml").run());
    }
}
