package com.dajudge.kindcontainer;

import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;

public class WaitForKubeApiStrategy extends HostPortWaitStrategy {
    private final WaitStrategy delegate;

    public WaitForKubeApiStrategy(final WaitStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public void waitUntilReady() {
        super.waitUntilReady();
        delegate.waitUntilReady(waitStrategyTarget);
    }

    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(singletonList(waitStrategyTarget.getMappedPort(6443)));
    }
}
