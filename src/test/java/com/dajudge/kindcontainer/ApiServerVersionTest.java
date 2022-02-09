package com.dajudge.kindcontainer;

import org.junit.runners.Parameterized;

import java.util.Collection;

public class ApiServerVersionTest extends AbstractVersionedTest {
    @Parameterized.Parameters
    public static Collection<ApiServerContainer.Version> apiServers() {
        return ApiServerContainer.Version.descending();
    }

    public ApiServerVersionTest(final ApiServerContainer.Version version) {
        super(() -> createApiServerContainer(version), version.descriptor);
    }

    private static ApiServerContainer<?> createApiServerContainer(final ApiServerContainer.Version version) {
        return new ApiServerContainer<>(version);
    }
}
