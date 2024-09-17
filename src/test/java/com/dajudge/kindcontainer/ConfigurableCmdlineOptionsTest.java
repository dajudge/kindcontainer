package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.k3sContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurableCmdlineOptionsTest {
    @TestFactory
    public Stream<DynamicTest> adds_cmdline_options() {
        return k3sContainers(this::assertCmdlineOptions);
    }

    private void assertCmdlineOptions(final KubernetesTestPackage<? extends K3sContainer<?>> container) {
        runWithK8s(container.newContainer()
                .withCommandLineModifier(cmdLine -> {
                    cmdLine.addAll(asList("--debug", "-v", "1"));
                    return cmdLine;
                }), k8s -> {
            List<String> commandParts = Arrays.stream(k8s.getCommandParts()).collect(Collectors.toList());
            // assert that default cmdline options are still present
            assertEquals("server", commandParts.get(0));
            assertTrue(commandParts.get(1).contains("traefik"));
            // assert that custom cmdline options are present
            assertTrue(commandParts.contains("--debug"));
            assertTrue(commandParts.contains("-v"));
            assertTrue(commandParts.contains("1"));
        });
    }
}
