package com.dajudge.kindcontainer;

public class NullWaitStrategy extends org.testcontainers.containers.wait.strategy.AbstractWaitStrategy {
    public static final NullWaitStrategy INSTANCE = new NullWaitStrategy();

    private NullWaitStrategy() {
    }

    @Override
    protected void waitUntilReady() {
    }
}
