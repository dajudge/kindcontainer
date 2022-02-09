package com.dajudge.kindcontainer;

import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.util.List;

import static java.util.Arrays.asList;

public class CompositeWaitStrategy extends AbstractWaitStrategy {
    private final List<AbstractWaitStrategy> children;

    public CompositeWaitStrategy(final List<AbstractWaitStrategy> children) {
        this.children = children;
    }

    @Override
    protected void waitUntilReady() {
        children.forEach(it -> it.waitUntilReady(waitStrategyTarget));
    }

    public static CompositeWaitStrategy allOf(final AbstractWaitStrategy... children) {
        return new CompositeWaitStrategy(asList(children));
    }
}
