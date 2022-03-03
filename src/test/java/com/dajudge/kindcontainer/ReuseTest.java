package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(Parameterized.class)
public class ReuseTest {
    @Parameterized.Parameters
    public static Collection<Supplier<KubernetesContainer<?>>> apiServers() {
        return Arrays.asList(
                // TODO ApiServerContainer::new,
                // TODO KindContainer::new,
                K3sContainer::new
        );
    }

    protected final Supplier<KubernetesContainer<?>> k8sFactory;

    public ReuseTest(final Supplier<KubernetesContainer<?>> k8sFactory) {
        this.k8sFactory = k8sFactory;
    }

    @Test
    public void does_not_reapply_postAvailabilityExecutions() {
        try (final KubernetesContainer<?> orig = applyConfig(k8sFactory.get())) {
            orig.start();
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(orig.getKubeconfig()))) {
                assertNotNull(client.serviceAccounts().inNamespace("my-namespace").withName("my-service-account").get());
                client.serviceAccounts().inNamespace("my-namespace").withName("my-service-account").delete();
            }
            final KubernetesContainer<?> copy = applyConfig(k8sFactory.get());
            copy.start();
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(copy.getKubeconfig()))) {
                assertNotNull(client.namespaces().withName("my-namespace").get());
                assertNull(client.serviceAccounts().inNamespace("my-namespace").withName("my-service-account").get());
            }
        }
    }

    public KubernetesContainer<?> applyConfig(final KubernetesContainer<?> k8s) {
        return k8s.withReuse(true)
                .withKubectl(kubectl -> kubectl.apply.fileFromClasspath("manifests/serviceaccount1.yaml").run());
    }
}
