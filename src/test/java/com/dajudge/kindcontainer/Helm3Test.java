package com.dajudge.kindcontainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.Assert.assertFalse;

@RunWith(Parameterized.class)
public class Helm3Test {
    @Parameterized.Parameters
    public static Collection<Supplier<KubernetesContainer<?>>> apiServers() {
        return Arrays.asList(ApiServerContainer::new, KindContainer::new);
    }

    private final KubernetesContainer<?> k8s;

    public Helm3Test(final Supplier<KubernetesContainer<?>> k8s) {
        this.k8s = k8s.get().withHelm3(helm -> {
            helm.repo.add.run("mittwald", "https://helm.mittwald.de");
            helm.repo.update.run();
            helm.install
                    .namespace("kubernetes-replicator")
                    .createNamespace()
                    .run("kubernetes-replicator", "mittwald/kubernetes-replicator");
        });
    }

    @Before
    public void before() {
        k8s.start();
    }

    @After
    public void after() {
        k8s.stop();
    }

    @Test
    public void can_install_something() {
        runWithClient(k8s, client -> {
            assertFalse(client.apps().deployments().inNamespace("kubernetes-replicator").list().getItems().isEmpty());
        });
    }
}
