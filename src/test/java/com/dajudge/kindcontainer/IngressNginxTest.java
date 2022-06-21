package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.ContainerVersionHelpers;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.DeploymentAvailableWaitStrategy.deploymentIsAvailable;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.CONTAINERS_WITH_KUBELET;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static io.fabric8.kubernetes.client.internal.readiness.Readiness.isDeploymentReady;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngressNginxTest {

    @ParameterizedTest
    @MethodSource(CONTAINERS_WITH_KUBELET)
    public void ingress_deployment_becomes_ready(final Supplier<KubernetesContainer<?>> factory) {
        ContainerVersionHelpers.runWithK8s(createContainer(factory), k8s -> runWithClient(k8s, client -> {
            final Deployment deployment = client.apps().deployments()
                    .inNamespace("ingress-nginx")
                    .withName("ingress-nginx-controller")
                    .get();
            assertNotNull(deployment);
            assertTrue(isDeploymentReady(deployment));
        }));
    }

    private static KubernetesContainer<?> createContainer(final Supplier<KubernetesContainer<?>> factory) {
        return factory.get().withHelm3(helm -> {
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
