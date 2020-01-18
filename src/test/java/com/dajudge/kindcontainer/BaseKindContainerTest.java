package com.dajudge.kindcontainer;

import static java.lang.Runtime.getRuntime;

abstract class BaseKindContainerTest {
    static final KindContainer K8S = createContainer();

    private static KindContainer createContainer() {
        final KindContainer container = new KindContainer()
                .withExposedPorts(30000)
                .waitingFor(NullWaitStrategy.INSTANCE)
                .withPodSubnet("10.245.0.0/16")
                .withServiceSubnet("10.112.0.0/12");
        container.start();
        getRuntime().addShutdownHook(new Thread(container::stop));
        return container;
    }
}
