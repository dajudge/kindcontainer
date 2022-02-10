package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.junit.ClassRule;
import org.junit.Test;

import static com.dajudge.kindcontainer.DeploymentAvailableWaitStrategy.deploymentIsAvailable;
import static com.dajudge.kindcontainer.TestUtils.runWithClient;
import static io.fabric8.kubernetes.client.internal.readiness.Readiness.isDeploymentReady;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IngressNginxTest {

    @ClassRule
    public static KindContainer<?> KIND = new KindContainer<>()
            .withHelm3(helm -> {
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

    @Test
    public void ingress_deployment_becomes_ready() throws Exception {
        runWithClient(KIND, client -> {
            final Deployment deployment = client.apps().deployments()
                    .inNamespace("ingress-nginx")
                    .withName("ingress-nginx-controller")
                    .get();
            assertNotNull(deployment);
            assertTrue(isDeploymentReady(deployment));
        });
    }
}
