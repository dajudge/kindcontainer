/*
Copyright 2020-2022 Alex Stockinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.dajudge.kindcontainer.kubecontrollermanager;

import com.dajudge.kindcontainer.client.TinyK8sClient;
import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;
import com.dajudge.kindcontainer.client.model.reflection.ApiResource;
import com.dajudge.kindcontainer.client.model.reflection.ApiResourceList;
import com.dajudge.kindcontainer.client.model.reflection.ApiVersions;
import com.dajudge.kindcontainer.client.model.v1.Namespace;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import static java.lang.String.format;
import static org.junit.Assert.assertNotNull;

/**
 * Make all resources in a namespace be gone.
 *
 * @link https://github.com/kubernetes/client-go/blob/8f44946f6cbe967fbe2e2548e76987680a89428e/discovery/helper.go#L87
 */
class NamespaceResourceReaper {
    private final TinyK8sClient client;

    NamespaceResourceReaper(final TinyK8sClient client) {
        this.client = client;
    }

    boolean ensureNamespaceIsEmpty(final Namespace namespace) {
        return ensureNamespaceIsVoidOfCoreResource(namespace)
                && ensureNamespaceIsVoidOfApiResources(namespace);
    }

    private boolean ensureNamespaceIsVoidOfCoreResource(final Namespace namespace) {
        final ApiVersions apiVersions = client.reflection().apiVersions().get();
        return apiVersions.getVersions().stream()
                .map(it -> ensureNamespaceIsVoidOfCoreResource(namespace, it))
                .reduce(true, (a, b) -> a && b);
    }

    private boolean ensureNamespaceIsVoidOfCoreResource(final Namespace namespace, final String version) {
        final ApiResourceList resources = client.reflection().coreApiResources(version).get();
        return resources.getResources().stream()
                .map(it -> ensureNamespaceIsVoidOf(namespace, null, it))
                .reduce(true, (a, b) -> a && b);
    }

    private boolean ensureNamespaceIsVoidOfApiResources(final Namespace namespace) {
        return client.reflection().apiGroups().get().getGroups().stream()
                .map(it -> ensureNamespaceIsVoidOf(namespace, it.getPreferredVersion().getGroupVersion()))
                .reduce(true, (a, b) -> a && b);
    }

    private Boolean ensureNamespaceIsVoidOf(final Namespace namespace, final String groupVersion) {
        return client.reflection().apiResources(groupVersion).get().getResources().stream()
                .map(it -> ensureNamespaceIsVoidOf(namespace, groupVersion, it))
                .reduce(true, (a, b) -> a && b);
    }

    private boolean ensureNamespaceIsVoidOf(final Namespace namespace, final String groupVersion, final ApiResource resource) {
        if (!resource.getNamespaced()) {
            return true;
        }
        if(resource.getName().contains("/")) {
            return true;
        }
        if (!resource.getVerbs().contains("delete")) {
            return true;
        }
        final String path = groupVersion == null ?
                format("/api/v1/namespaces/%s/%s",
                        namespace.getMetadata().getName(),
                        resource.getName()) :
                format("/apis/%s/namespaces/%s/%s",
                        groupVersion,
                        namespace.getMetadata().getName(),
                        resource.getName());
        final ResourceList<? extends WithMetadata> result = client.http().syncGet(
                path,
                new TypeReference<ResourceList<WithMetadata>>() {
                }
        );
        assertNotNull(result);
        if (result.getItems() == null || result.getItems().isEmpty()) {
            return true;
        }
        result.getItems().forEach(it -> client.http().syncDelete(
                format("%s/%s", path, it.getMetadata().getName()),
                new TypeReference<WithMetadata>() {
                })
        );
        return false;
    }
}
