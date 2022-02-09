package com.dajudge.kindcontainer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

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
            k8s.runWithClient(client -> {
                assertEquals(client.getKubernetesVersion().getGitVersion(), version.getKubernetesVersion());
            });
        } catch (final Exception e) {
            throw new AssertionError("Failed to launch kubernetes with version " + version.toString(), e);
        } finally {
            k8s.stop();
        }
    }
}
