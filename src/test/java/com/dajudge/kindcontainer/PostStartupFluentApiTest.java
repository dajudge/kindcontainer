package com.dajudge.kindcontainer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostStartupFluentApiTest {
    private static ApiServerContainer<?> k8s = new ApiServerContainer<>();

    @BeforeAll
    public static void beforeAll() {
        k8s.start();
    }

    @AfterAll
    public static void afterAll() {
        k8s.stop();
    }

    @Test
    public void executes_withKubectl_after_startup() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        k8s.withKubectl(kubectl -> {
            executed.set(true);
        });
        assertTrue(executed.get());
    }

    @Test
    public void executes_withHelm3_after_startup() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        k8s.withHelm3(helm -> {
            executed.set(true);
        });
        assertTrue(executed.get());
    }

    @Test
    public void executes_withKubeconfig_after_startup() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        k8s.withKubeconfig(kubeconfig -> {
            executed.set(true);
        });
        assertTrue(executed.get());
    }
}
