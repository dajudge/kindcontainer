package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.dajudge.kindcontainer.DeploymentAvailableWaitStrategy.deploymentIsAvailable;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static io.fabric8.kubernetes.client.internal.readiness.Readiness.isDeploymentReady;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class IngressNginxTest extends BaseFullContainersTest {

    public IngressNginxTest(final KubernetesWithKubeletContainer<?> k8s) {
        super(k8s.withHelm3(helm -> {
                    helm.repo.add.run("ingress-nginx", "https://kubernetes.github.io/ingress-nginx");
                    helm.repo.update.run();
                    helm.install
                            .namespace("ingress-nginx")
                            .createNamespace(true)
                            .set("controller.service.type", "NodePort")
                            .set("controller.service.nodePorts.http", "30080")
                            .run("ingress-nginx", "ingress-nginx/ingress-nginx");
                })
                .waitingFor(deploymentIsAvailable("ingress-nginx", "ingress-nginx-controller")));
    }

    @Test
    public void ingress_deployment_becomes_ready() {
        runWithClient(k8s, client -> {
            final Deployment deployment = client.apps().deployments()
                    .inNamespace("ingress-nginx")
                    .withName("ingress-nginx-controller")
                    .get();
            assertNotNull(deployment);
            assertTrue(isDeploymentReady(deployment));
        });
    }
}
