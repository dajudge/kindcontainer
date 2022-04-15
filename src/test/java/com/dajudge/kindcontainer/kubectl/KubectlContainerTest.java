package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.dajudge.kindcontainer.K3sContainer;
import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KubernetesContainer;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class KubectlContainerTest {
    @Parameterized.Parameters
    public static Collection<Supplier<KubernetesContainer<?>>> apiServers() {
        return Arrays.asList(ApiServerContainer::new, K3sContainer::new, KindContainer::new);
    }

    protected final Supplier<KubernetesContainer<?>> k8sFactory;

    public KubectlContainerTest(final Supplier<KubernetesContainer<?>> k8sFactory) {
        this.k8sFactory = k8sFactory;
    }

    @Test
    public void kubectl_works() {
        try (final KubernetesContainer<?> k8s = createK8s()) {
            k8s.start();
            try (final DefaultKubernetesClient client = createClient(k8s)) {
                assertNotNull(client.inNamespace("my-namespace").serviceAccounts().withName("my-service-account").get());
            }
        }
    }

    @NotNull
    private DefaultKubernetesClient createClient(KubernetesContainer<?> k8s) {
        return new DefaultKubernetesClient(fromKubeconfig(k8s.getKubeconfig()));
    }

    private KubernetesContainer<?> createK8s() {
        return k8sFactory.get()
                .withKubectl(kubectl -> kubectl.apply.fileFromClasspath("manifests/serviceaccount1.yaml").run());
    }
}
