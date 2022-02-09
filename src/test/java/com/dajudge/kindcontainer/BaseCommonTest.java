package com.dajudge.kindcontainer;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public abstract class BaseCommonTest {
    @Parameterized.Parameters
    public static Collection<Object[]> apiServers() {
        return Arrays.asList(new Object[][]{{StaticContainers.apiServer()}, {StaticContainers.kind()}});
    }

    protected final KubernetesContainer<?> k8s;

    protected BaseCommonTest(final KubernetesContainer<?> k8s) {
        this.k8s = k8s;
    }
}
