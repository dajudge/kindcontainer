package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.ApiServerContainer;

import java.util.stream.Stream;

public class ApiServerVersionTest extends AbstractVersionedTest {
    @Override
    protected Stream<KubernetesTestPackage<?>> testPackages() {
        return ApiServerContainer.Version.descending().stream()
                .map(version -> new KubernetesTestPackage<ApiServerContainer<?>>(
                        ApiServerContainer.class.getSimpleName(),
                        () -> new ApiServerContainer<>(version),
                        version.getDescriptor()
                ));
    }
}
