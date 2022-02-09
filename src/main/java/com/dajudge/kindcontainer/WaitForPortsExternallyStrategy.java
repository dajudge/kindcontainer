package com.dajudge.kindcontainer;

import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class WaitForPortsExternallyStrategy extends AbstractWaitStrategy {

    @Override
    public void waitUntilReady() {
        final Set<Integer> ports = waitStrategyTarget.getExposedPorts()
                .stream()
                .map(waitStrategyTarget::getMappedPort)
                .collect(toSet());
        final ExternalPortListeningCheck check = new ExternalPortListeningCheck(waitStrategyTarget, ports);
        Awaitility.await()
                .pollInSameThread()
                .pollInterval(Duration.ofMillis(100))
                .pollDelay(Duration.ZERO)
                .ignoreExceptions()
                .forever()
                .until(check);
    }
}

