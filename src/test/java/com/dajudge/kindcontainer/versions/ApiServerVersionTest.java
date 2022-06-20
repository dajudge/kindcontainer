package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.dajudge.kindcontainer.ApiServerContainerVersion;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.descending;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.versionFilter;

public class ApiServerVersionTest extends AbstractVersionedTest {
    @Override
    protected Stream<KubernetesTestPackage<?>> testPackages() {
        return descending(ApiServerContainerVersion.class).stream()
                .filter(versionFilter(ApiServerContainer.class.getSimpleName(), "all"))
                .map(version -> new KubernetesTestPackage<ApiServerContainer<?>>(
                        ApiServerContainer.class.getSimpleName(),
                        () -> new ApiServerContainer<>(version),
                        version.descriptor()
                ));
    }
}
