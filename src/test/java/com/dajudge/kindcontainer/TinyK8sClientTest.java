package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.client.*;
import com.dajudge.kindcontainer.client.model.base.Metadata;
import com.dajudge.kindcontainer.client.model.base.ResourceAction;
import com.dajudge.kindcontainer.client.model.v1.Namespace;
import com.dajudge.kindcontainer.client.model.v1.ServiceAccount;
import org.junit.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.*;

public class TinyK8sClientTest {
    private static final ApiServerContainer<?> K8S = StaticContainers.apiServer();

    private final TinyK8sClient client = TinyK8sClient.fromKubeconfig(K8S.getKubeconfig());

    @Test
    public void can_list_namespaces() {
        final List<Namespace> namespaces = client.v1().namespaces().list().getItems();
        final List<String> names = namespaces.stream().map(it -> it.getMetadata().getName()).collect(toList());
        assertTrue(names.toString(), names.contains("kube-system"));
    }

    @Test
    public void can_create_namespace() {
        final Namespace namespace = createNamespace(client);
        final List<Namespace> namespaces = client.v1().namespaces().list().getItems();
        final List<String> names = namespaces.stream().map(it -> it.getMetadata().getName()).collect(toList());
        assertTrue(names.toString(), names.contains(namespace.getMetadata().getName()));
    }

    @Test
    public void can_delete_namespace() {
        final Namespace namespace = createNamespace(client);
        final Namespace deletedNamespace = client.v1().namespaces().delete(namespace.getMetadata().getName());
        deletedNamespace.getSpec().setFinalizers(emptyList());
        client.v1().namespaces().finalizeNamespace(deletedNamespace);
        Awaitility.await()
                .timeout(10, SECONDS)
                .pollInSameThread()
                .until(() -> client.v1().namespaces().find(namespace.getMetadata().getName()), o -> !o.isPresent());
    }

    @Test
    public void can_create_service_account() {
        final Namespace namespace = createNamespace(client);
        final ServiceAccount sa = createServiceAccount(client, namespace);
        assertNotNull(client.v1().serviceAccounts().inNamespace(namespace.getMetadata().getName()).find(sa.getMetadata().getName()));
    }

    @Test
    public void can_watch_all_serviceaccounts() {
        final Namespace namespace1 = createNamespace(client);
        final Namespace namespace2 = createNamespace(client);
        assertWatchWorks(
                asList(namespace1, namespace2),
                asList(namespace1, namespace2),
                client.v1().serviceAccounts()
        );
    }

    @Test
    public void can_watch_namespaced_serviceaccounts() {
        final Namespace namespace1 = createNamespace(client);
        final Namespace namespace2 = createNamespace(client);
        assertWatchWorks(
                asList(namespace1, namespace2),
                singletonList(namespace1),
                client.v1().serviceAccounts().inNamespace(namespace1.getMetadata().getName())
        );
    }

    private void assertWatchWorks(
            final List<Namespace> allNamespaces,
            final List<Namespace> watchedNamespaces,
            final Watchable<ServiceAccount, ServiceAccount.ItemList, ServiceAccount.StreamItem> watchable
    ) {
        final Map<Namespace, ServiceAccount> existingAccounts = allNamespaces.stream()
                .collect(toMap(ns -> ns, ns -> createServiceAccount(client, ns)));
        final Collection<String> seenAccounts = synchronizedSet(new HashSet<>());
        final Watch watch = watchable.watch(new WatchCallback<ServiceAccount>() {
            @Override
            public void onAction(ResourceAction action, ServiceAccount resource) {
                seenAccounts.add(resource.getMetadata().getName());
            }

            @Override
            public void onClose() {

            }
        });
        try {
            watchedNamespaces.forEach(ns -> {
                assertServiceAccountIsSeen(seenAccounts, existingAccounts.get(ns).getMetadata().getName());
            });
            allNamespaces.stream().filter(it -> !watchedNamespaces.contains(it)).forEach(ns -> {
                assertServiceAccountUnseen(seenAccounts, existingAccounts.get(ns).getMetadata().getName());
            });
            final Map<Namespace, ServiceAccount> newAccounts = allNamespaces.stream()
                    .collect(toMap(ns -> ns, ns -> createServiceAccount(client, ns)));
            watchedNamespaces.forEach(ns -> {
                assertServiceAccountIsSeen(seenAccounts, newAccounts.get(ns).getMetadata().getName());
            });
            allNamespaces.stream().filter(it -> !watchedNamespaces.contains(it)).forEach(ns -> {
                assertServiceAccountUnseen(seenAccounts, newAccounts.get(ns).getMetadata().getName());
            });
        } finally {
            watch.close();
        }
    }

    private void assertServiceAccountUnseen(Collection<String> seenAccounts, String account) {
        assertFalse(seenAccounts.contains(account));
    }

    private void assertServiceAccountIsSeen(Collection<String> seenAccounts, String account) {
        Awaitility.await()
                .timeout(10, SECONDS)
                .pollInSameThread()
                .until(() -> seenAccounts.contains(account));
    }

    private ServiceAccount createServiceAccount(final TinyK8sClient client, final Namespace namespace) {
        final ServiceAccount serviceAccount = new ServiceAccount();
        serviceAccount.setMetadata(new Metadata());
        serviceAccount.getMetadata().setName(UUID.randomUUID().toString());
        serviceAccount.getMetadata().setNamespace(namespace.getMetadata().getName());
        final ServiceAccount createdServiceAccount = client.v1().serviceAccounts()
                .inNamespace(namespace.getMetadata().getName())
                .create(serviceAccount);
        assertNotNull(createdServiceAccount);
        return createdServiceAccount;
    }

    private Namespace createNamespace(final TinyK8sClient client) {
        final Namespace namespace = new Namespace();
        namespace.setMetadata(new Metadata());
        namespace.getMetadata().setName(UUID.randomUUID().toString());
        final Namespace createdNamespace = client.v1().namespaces().create(namespace);
        assertNotNull(createdNamespace);
        return createdNamespace;
    }
}
