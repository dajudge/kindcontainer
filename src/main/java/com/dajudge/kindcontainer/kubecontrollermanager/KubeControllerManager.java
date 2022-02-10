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
import com.dajudge.kindcontainer.client.Watch;
import com.dajudge.kindcontainer.client.WatchCallback;
import com.dajudge.kindcontainer.client.model.base.Metadata;
import com.dajudge.kindcontainer.client.model.base.ResourceAction;
import com.dajudge.kindcontainer.client.model.v1.Namespace;
import com.dajudge.kindcontainer.client.model.v1.ServiceAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Phaser;

/**
 * An implement-as-you-need implementation of some things kube-controller-manager is doing to
 * make the usage of <code>ApiServerContainer</code> more real-world-kubernetes-like.
 */
public class KubeControllerManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(KubeControllerManager.class);
    private final Phaser phaser = new Phaser(1);
    private final Collection<Watch> watches = new ArrayList<>();
    private final Collection<Job> jobs = new ArrayList<>();

    public void start(final TinyK8sClient client) {
        watches.add(client.v1().namespaces().watch(new WatchCallback<Namespace>() {
            @Override
            public void onAction(final ResourceAction action, final Namespace resource) {
                try {
                    phaser.register();
                    onNamespaceEvent(client, action, resource);
                } finally {
                    phaser.arriveAndDeregister();
                }
            }

            @Override
            public void onClose() {
                phaser.arriveAndAwaitAdvance();
            }
        }));
    }

    private void onNamespaceEvent(
            final TinyK8sClient client,
            final ResourceAction action,
            final Namespace namespace
    ) {
        LOG.info("{} {}", action, namespace);
        switch (action) {
            case ADDED:
            case MODIFIED:
                doSafe(action, namespace, () -> ensureServiceAccountWhenActive(client, namespace));
                doSafe(action, namespace, () -> removeFinalizerWhenTerminating(client, namespace));
                break;
        }
    }

    private void doSafe(final ResourceAction action, final Namespace resource, final Runnable runnable) {
        try {
            runnable.run();
        } catch (final Exception e) {
            LOG.warn(
                    "Unexpected exception handling event {} {} {}",
                    action,
                    resource.getKind(),
                    resource.getMetadata().getName(),
                    e
            );
        }
    }

    private void removeFinalizerWhenTerminating(final TinyK8sClient client, final Namespace namespace) {
        if (namespace.getMetadata().getDeletionTimestamp() == null) {
            return;
        }
        final NamespaceResourceReaper reaper = new NamespaceResourceReaper(client);
        final Thread thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                if (reaper.ensureNamespaceIsEmpty(namespace)) {
                    LOG.info("Finalizing namespace {}", namespace.getMetadata().getName());
                    Optional.ofNullable(namespace.getSpec().getFinalizers())
                            .ifPresent(it -> it.remove("kubernetes"));
                    client.v1().namespaces().finalizeNamespace(namespace);
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    return;
                }
            }
        });
        thread.start();
        synchronized (jobs) {
            jobs.add(() -> {
                thread.interrupt();
                try {
                    thread.join(10000);
                } catch (final InterruptedException e) {
                    LOG.warn("Failed to join Namespace finalizer job", e);
                }
            });
        }
    }

    private void ensureServiceAccountWhenActive(final TinyK8sClient client, final Namespace namespace) {
        if (namespace.getMetadata().getDeletionTimestamp() != null) {
            return;
        }
        final Optional<ServiceAccount> existingServiceAccount = client.v1().serviceAccounts()
                .inNamespace(namespace.getMetadata().getName())
                .find("default");
        if (existingServiceAccount.isPresent()) {
            return;
        }
        LOG.info("Creating default service account for namespace {}...", namespace.getMetadata().getName());
        final ServiceAccount account = new ServiceAccount();
        account.setMetadata(new Metadata());
        account.getMetadata().setName("default");
        client.v1().serviceAccounts()
                .inNamespace(namespace.getMetadata().getName())
                .create(account);
    }

    @Override
    public void close() {
        watches.forEach(Watch::close);
        jobs.forEach(Job::close);
        LOG.info("{} stopped.", KubeControllerManager.class.getSimpleName());
    }

    interface Job extends AutoCloseable {
        @Override
        void close();
    }
}
