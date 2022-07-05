package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.apiServerContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubectlCreateSecretFluentTest {

    @TestFactory
    public Stream<DynamicTest> creates_docker_secret() {
        return apiServerContainers(this::assertCreatesDockerSecret);
    }

    private void assertCreatesDockerSecret(ApiServerContainer<?> container) {
        runWithK8s(configureContainer(container), k8s -> runWithClient(k8s, client -> {
            assertNotNull(client.inNamespace("default").secrets().withName("pull-secret").get());
        }));
    }

    private ApiServerContainer<?> configureContainer(final ApiServerContainer<?> container) {
        return container.withKubectl(kubectl -> kubectl.create.secret.dockerRegistry
                .namespace("default")
                .dockerEmail("tstark@example.com")
                .dockerServer("https://registry.example.com")
                .dockerUsername("tony")
                .dockerPassword("p3pp3er")
                .run("pull-secret"));
    }
}
