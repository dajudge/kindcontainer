package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.util.ContainerVersionHelpers;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.allContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubectlContainerTest {

    @TestFactory
    public Stream<DynamicTest> kubectl_works() {
        return allContainers(this::assertKubectlWorks);
    }

    private void assertKubectlWorks(ContainerVersionHelpers.KubernetesTestPackage<?> pkg) {
        runWithK8s(createK8s(pkg.newContainer()), k8s -> runWithClient(k8s, client -> {
            assertNotNull(client.inNamespace("my-namespace").serviceAccounts().withName("my-service-account").get());
        }));
    }

    private KubernetesContainer<?> createK8s(final KubernetesContainer<?> k8s) {
        return k8s.withKubectl(kubectl -> kubectl.apply.fileFromClasspath("manifests/serviceaccount1.yaml").run());
    }
}
