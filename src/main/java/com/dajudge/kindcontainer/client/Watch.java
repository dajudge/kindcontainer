package com.dajudge.kindcontainer.client;

public interface Watch extends AutoCloseable {
    Watch CLOSED = new Watch() {
        @Override
        public void close() {
        }

        @Override
        public void await() {
        }
    };

    @Override
    void close();

    void await() throws InterruptedException;
}
