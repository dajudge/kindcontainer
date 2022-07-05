package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.DeploymentAvailableWaitStrategy.deploymentIsAvailable;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.kubeletContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static io.fabric8.kubernetes.client.internal.readiness.Readiness.isDeploymentReady;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngressNginxTest {

    @TestFactory
    public Stream<DynamicTest> ingress_deployment_becomes_ready() {
        return kubeletContainers(k8s -> runWithK8s(configureContainer(k8s), this::assertIngressDeploymentBecomesReady));
    }

    private void assertIngressDeploymentBecomesReady(KubernetesContainer<?> k8s) {
        runWithClient(k8s, client -> {
            final Deployment deployment = client.apps().deployments()
                    .inNamespace("ingress-nginx")
                    .withName("ingress-nginx-controller")
                    .get();
            assertNotNull(deployment);
            assertTrue(isDeploymentReady(deployment));
        });
    }

    private static KubernetesContainer<?> configureContainer(final KubernetesContainer<?> container) {
        return container.withHelm3(helm -> {
                    helm.repo.add.run("ingress-nginx", "https://kubernetes.github.io/ingress-nginx");
                    helm.repo.update.run();
                    helm.install
                            .namespace("ingress-nginx")
                            .createNamespace(true)
                            .set("controller.service.type", "NodePort")
                            .set("controller.service.nodePorts.http", "30080")
                            .run("ingress-nginx", "ingress-nginx/ingress-nginx");
                })
                .waitingFor(deploymentIsAvailable("ingress-nginx", "ingress-nginx-controller"));
    }
}
