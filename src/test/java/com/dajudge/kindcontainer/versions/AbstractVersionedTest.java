package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.KubernetesVersionDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public abstract class AbstractVersionedTest {

    protected final Supplier<KubernetesContainer<?>> k8sFactory;
    protected final KubernetesVersionDescriptor version;

    protected AbstractVersionedTest(
            final Supplier<KubernetesContainer<?>> k8sFactory,
            final KubernetesVersionDescriptor version
    ) {
        this.k8sFactory = k8sFactory;
        this.version = version;
    }

    @Test
    public void can_start() {
        final KubernetesContainer<?> k8s = k8sFactory.get();
        try {
            k8s.start();
            runWithClient(k8s, client -> {
                final String gitVersion = client.getKubernetesVersion().getGitVersion();  // e.g. v1.21.9+k3s1
                final String k8sVersion = gitVersion.replaceAll("\\+.*", ""); // e.g. v1.21.9
                assertEquals(version.getKubernetesVersion(), k8sVersion);
            });
        } catch (final Exception e) {
            throw new AssertionError("Failed to launch kubernetes with version " + version.getKubernetesVersion(), e);
        } finally {
            k8s.stop();
        }
    }
}
