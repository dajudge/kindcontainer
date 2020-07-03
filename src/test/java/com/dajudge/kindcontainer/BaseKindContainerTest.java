package com.dajudge.kindcontainer;

import static com.dajudge.kindcontainer.TestUtils.stringResource;
import static java.lang.Runtime.getRuntime;
import static java.util.Collections.singletonList;

abstract class BaseKindContainerTest {
    static final KindContainer K8S = createContainer();
    final String namespace = K8S.withClient(TestUtils::createNewNamespace);

    private static KindContainer createContainer() {
        final KindContainer container = new KindContainer()
                .withExposedPorts(30000)
                .waitingFor(NullWaitStrategy.INSTANCE)
                .withPodSubnet("10.245.0.0/16")
                .withServiceSubnet("10.112.0.0/12")
                .withCaCerts(singletonList(stringResource("test.crt")));
        container.start();
        getRuntime().addShutdownHook(new Thread(container::stop));
        return container;
    }
}
