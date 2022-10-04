package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.ContainerVersionHelpers;
import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.ApiServerContainerVersion.VERSION_1_24_1;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.allContainers;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageOverrideTest {
    private static final int NGINX_PORT = 8080;


    @TestFactory
    public Stream<DynamicTest> starts_with_custom_image() {
        return allContainers(this::assertUsesCustomDockerImage);
    }

    public void assertUsesCustomDockerImage(final KubernetesTestPackage<? extends KubernetesContainer<?>> testPkg) {
        final KubernetesVersionEnum<?> version = testPkg.version();
        final String registry = version.defaultImageTemplate()
                .replaceAll("/.*", "");
        try (final GenericContainer<?> nginx = createNginx(registry)) {
            nginx.start();
            final String image = version.defaultImageTemplate()
                    .replace("${major}", String.valueOf(version.descriptor().getMajor()))
                    .replace("${minor}", String.valueOf(version.descriptor().getMinor()))
                    .replace("${patch}", String.valueOf(version.descriptor().getPatch()))
                    .replaceAll("^[^/]+", String.format("localhost:%d", nginx.getMappedPort(NGINX_PORT)));
            try (final KubernetesContainer<?> k8s = testPkg.withImage(image).newContainer()) {
                k8s.start();
                assertTrue(k8s.getImage().get().startsWith(String.format("localhost:%d/", nginx.getMappedPort(NGINX_PORT))));
            }
        }
    }

    private static GenericContainer<?> createNginx(final String registryToProxy) {
        return new GenericContainer<>("nginx:1.21.3")
                .withExposedPorts(NGINX_PORT)
                .withCopyToContainer(MountableFile.forClasspathResource("nginx-reverse-proxy.conf"), "/etc/nginx/templates/default.conf.template")
                .withEnv("NGINX_PORT", String.format("%d", NGINX_PORT))
                .withEnv("REGISTRY_TO_PROXY", registryToProxy)
                .waitingFor(new HttpWaitStrategy().forPort(NGINX_PORT).forStatusCodeMatching(code -> code >= 200 && code < 400));
    }
}
