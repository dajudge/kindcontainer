package com.dajudge.kindcontainer;

import org.junit.ClassRule;
import org.junit.Test;

import static com.dajudge.kindcontainer.TestUtils.runWithClient;
import static org.junit.Assert.assertFalse;

public class Helm3Test {

    @ClassRule
    public static ApiServerContainer<?> K8S = new ApiServerContainer<>()
            .withHelm3(helm -> {
                helm.repo.add.run("mittwald", "https://helm.mittwald.de");
                helm.repo.update.run();
                helm.install
                        .namespace("kubernetes-replicator")
                        .createNamespace()
                        .run("kubernetes-replicator", "mittwald/kubernetes-replicator");
            });

    @Test
    public void can_install_something() {
        runWithClient(K8S, client -> {
            assertFalse(client.apps().deployments().inNamespace("kubernetes-replicator").list().getItems().isEmpty());
        });
    }
}
