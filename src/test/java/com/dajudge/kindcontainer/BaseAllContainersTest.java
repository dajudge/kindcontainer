package com.dajudge.kindcontainer;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public abstract class BaseAllContainersTest {
    @Parameterized.Parameters
    public static Collection<KubernetesContainer<?>> containers() {
        return asList(new ApiServerContainer<>(), new K3sContainer<>(), new KindContainer<>());
    }

    protected final KubernetesContainer<?> k8s;

    @Before
    public void beforeAllContainers() {
        k8s.start();
    }

    @After
    public void afterAllContainers() {
        k8s.stop();
    }

    protected BaseAllContainersTest(final KubernetesContainer<?> k8s) {
        this.k8s = k8s;
    }
}
