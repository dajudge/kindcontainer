package com.dajudge.kindcontainer;

import static com.dajudge.kindcontainer.TestUtils.stringResource;
import static java.util.Collections.singletonList;

/**
 * Manages containers for testing using the singleton pattern:
 * in https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/
 */
public class StaticContainers {

    private static KindContainer<?> kind;
    private static ApiServerContainer<?> apiServer;

    public synchronized static KindContainer<?> kind() {
        if (kind == null) {
            kind = new KindContainer<>()
                    .withExposedPorts(30000);
            kind.start();
            Runtime.getRuntime().addShutdownHook(new Thread(kind::close));
        }
        return kind;
    }

    public synchronized static ApiServerContainer<?> apiServer() {
        if (apiServer == null) {
            apiServer = new ApiServerContainer<>()
                    .withExposedPorts(30000);
            apiServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(apiServer::close));
        }
        return apiServer;
    }
}
