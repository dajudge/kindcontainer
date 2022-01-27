package com.dajudge.kindcontainer;

import org.junit.ClassRule;
import org.junit.runners.Parameterized;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ser.Serializers;

import java.util.Arrays;
import java.util.Collection;

public abstract class BaseCommonTest extends BaseKindContainerTest {
    @ClassRule
    public static ApiServerContainer<?> API_SERVER = new ApiServerContainer<>();

    @Parameterized.Parameters
    public static Collection<Object[]> apiServers() {
        return Arrays.asList(new Object[][] {
                { KIND },
                { API_SERVER }
        });
    }

    protected final KubernetesContainer<?> k8s;

    protected BaseCommonTest(final KubernetesContainer<?> k8s) {
        this.k8s = k8s;
    }
}
