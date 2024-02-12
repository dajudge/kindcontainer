[![CI](https://github.com/dajudge/kindcontainer/actions/workflows/build.yaml/badge.svg)](https://github.com/dajudge/kindcontainer/actions/workflows/build.yaml)
[![Maven central](https://img.shields.io/maven-central/v/com.dajudge.kindcontainer/kindcontainer)](https://search.maven.org/artifact/com.dajudge.kindcontainer/kindcontainer)

Kindcontainer
---
A Java-based [Testcontainers](https://www.testcontainers.org/) container implementation that provides ephemeral
Kubernetes clusters for integration testing.

<!-- TOC -->
  * [Kindcontainer](#kindcontainer)
* [Container Flavors](#container-flavors)
* [Usage](#usage)
  * [Add dependency](#add-dependency)
    * [Maven](#maven)
    * [Gradle](#gradle)
  * [Use in JUnit 4 test](#use-in-junit-4-test)
    * [With `KindContainer`](#with-kindcontainer)
    * [With `K3sContainer`](#with-k3scontainer)
    * [With `ApiServerContainer`](#with-apiservercontainer)
* [Quick guides](#quick-guides)
  * [Running different versions of Kubernetes](#running-different-versions-of-kubernetes)
  * [Using the `kubectl` and `helm` fluent APIs](#using-the-kubectl-and-helm-fluent-apis)
  * [Using custom docker images](#using-custom-docker-images)
    * [Kubernetes images](#kubernetes-images)
    * [`kubectl` and `helm` images for fluent APIs](#kubectl-and-helm-images-for-fluent-apis)
    * [`etcd` image for `ApiServerContainer`](#etcd-image-for-apiservercontainer)
    * [`sshd` and `nginx` image for webhook testing](#sshd-and-nginx-image-for-webhook-testing)
  * [Testing admission webhooks](#testing-admission-webhooks)
* [Examples](#examples)
<!-- TOC -->

# Container Flavors

The Kindcontainer libraries offers three different Kubernetes container implementations:

* `ApiServerContainer`
* `K3sContainer`
* `KindContainer`

While `ApiServerContainer` (as the name suggests) starts only a Kubernetes API Server (plus the required etcd),
both `K3sContainer` and `KindContainer` are feature rich Kubernetes containers that can e.g. spin up `Pods`
and even provision `PersistentVolumes`.

# Usage

## Add dependency

First you need to add the Kindcontainer dependency to your build. Kindcontainer is available on maven central.

### Maven

Add the Kindcontainer dependency:

```xml

<project>
    <dependencies>
        <dependency>
            <groupId>com.dajudge.kindcontainer</groupId>
            <artifactId>kindcontainer</artifactId>
            <version>1.4.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### Gradle

Add the Kindcontainer dependency:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    testImplementation "com.dajudge.kindcontainer:kindcontainer:1.4.0"
}
```

## Use in JUnit 4 test

Once you have the Kindcontainer dependency configured you can create JUnit test case easily.

### With `KindContainer`

```java
public class SomeKindTest {
    @ClassRule
    public static final KindContainer<?> KUBE = new KindContainer<>();

    @Test
    public void verify_node_is_present() {
        // Create a fabric8 client and use it!
        try (KubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(KUBE.getKubeconfig()))) {
            assertEquals(1, client.nodes().list().getItems().size());
        }
    }
}
```

Look [here](src/test/java/com/dajudge/kindcontainer/readme/junit4/SomeKindTest.java) for the reference test.

### With `K3sContainer`

```java
public class SomeK3sTest {
    @ClassRule
    public static final K3sContainer<?> K3S = new K3sContainer<>();

    @Test
    public void verify_node_is_present() {
        // Create a fabric8 client and use it!
        try (KubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(K3S.getKubeconfig()))) {
            assertEquals(1, client.nodes().list().getItems().size());
        }
    }
}
```

Look [here](src/test/java/com/dajudge/kindcontainer/readme/junit4/SomeK3sTest.java) for the reference test.

### With `ApiServerContainer`

If you don't need a full-fledged Kubernetes distribution for your testing, using the `ApiServerContainer`
might be an option for you that shaves off a lot of the startup overhead of the `KindContainer`. The
`ApiServerContainer` only starts a Kubernetes API-Server (and the required etcd), which can already be enough
if all you want to test is if your custom controller/operator handles its CRDs properly or creates the required
objects in the control plane.

```java
public class SomeApiServerTest {
    @ClassRule
    public static final ApiServerContainer<?> KUBE = new ApiServerContainer<>();

    @Test
    public void verify_no_node_is_present() {
        // Create a fabric8 client and use it!
        try (KubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(KUBE.getKubeconfig()))) {
            assertTrue(client.nodes().list().getItems().isEmpty());
        }
    }
}
```

Look [here](src/test/java/com/dajudge/kindcontainer/readme/junit4/SomeApiServerTest.java) for the reference test.

# Quick guides

Here's a couple challenges frequently seen in the wild and how you can solve them with Kindcontainer.

## Running different versions of Kubernetes

Kindcontainer supports running different (selected and tested) versions of Kubernetes. The default version is the
latest supported stable version for each container. You can change the version by passing a version enum to the
constructor. The following example illustrates this process for the `KindContainer` implementation, but it works
analogous for the other two containers as well.

```java
KindContainer<?> container=new KindContainer<>(KindContainerVersion.VERSION_1_24_1);
```

## Using the `kubectl` and `helm` fluent APIs

Kindcontainer makes it easy to perform common tasks either during setup of the container
or later on during the test by offering fluent APIs to the `kubectl` and `helm` commands.

If you're acquainted with the `kubectl` and `helm` commands, you'll feel right at home with
the fluent APIs in no time.

You can use them directly during container instantiation like this:

```java
// Kubectl example
public class SomeKubectlTest {
    @ClassRule
    public static final ApiServerContainer<?> KUBE = new ApiServerContainer<>()
            .withKubectl(kubectl -> {
                kubectl.apply
                        .fileFromClasspath("manifests/serviceaccount1.yaml")
                        .run();
            });
}

// Helm3 example
public class SomeHelmTest {
    @ClassRule
    public static final KindContainer<?> KUBE = new KindContainer<>()
            .withHelm3(helm -> {
                helm.repo.add.run("mittwald", "https://helm.mittwald.de");
                helm.repo.update.run();
                helm.install
                        .namespace("kubernetes-replicator")
                        .createNamespace()
                        .run("kubernetes-replicator", "mittwald/kubernetes-replicator");
            });
}
```

The fluent APIs are far from complete, but they cover the most common use cases. If you're
missing a command, feel free to open an issue or even better, a pull request.

## Using custom docker images

In some environments it might be necessary to use custom docker images for the containers Kindcontainer starts.

___Attention___: You need to make sure that the images you are using are compatible with the images used by
kindcontainer
by default. These are the images used by Kindcontainer if you don't override them:

|         Purpose         |              Image               |              Version               |
|:-----------------------:|:--------------------------------:|:----------------------------------:|
|  `ApiServerContainer`   | `registry.k8s.io/kube-apiserver` |   `v${major}.${minor}.${patch}`    |
|     `K3sContainer`      |          `rancher/k3s`           | `v${major}.${minor}.${patch}-k3s1` |
|     `KindContainer`     |          `kindest/node`          |   `v${major}.${minor}.${patch}`    |
|         `etcd`          |      `registry.k8s.io/etcd`      |             `3.4.13-0`             |
|    Fluent API `helm`    |          `alpine/helm`           |              `3.14.0`              |
|  Fluent API `kubectl`   |        `bitnami/kubectl`         |       `1.21.9-debian-10-r10`       |
|    Webhooks `nginx`     |             `nginx`              |              `1.23.3`              |
| Webhooks OpenSSH Server |   `linuxserver/openssh-server`   |          `9.0_p1-r2-ls99`          |         

### Kubernetes images

You can customize the docker image of the Kubernetes container you're starting. This can be
by suffixing the kubernetes version you want to run with a call to `withImage()` like this:

```java
KindContainer<?> container = new KindContainer<>(KindContainerVersion.VERSION_1_24_1.withImage("my-registry.com/kind:1.24.1"));
```

### `kubectl` and `helm` images for fluent APIs

The fluent APIs for  `helm` and `kubectl` are implemented using support containers. To customize which images are being
used to start those support containers you can use the `withKubectlImage()` and `withHelm3Image()` methods:

```java
K3sContainer<?> container = new K3sContainer<>()
        .withKubectlImage(DockerImageName.parse("my-registry/kubectl:1.21.9-debian-10-r10"))
        .withHelm3Image(DockerImageName.parse("my-registry/helm:3.7.2"));
```

### `etcd` image for `ApiServerContainer`

`ApiServerContainer` has a hard dependency on `etcd` that's started in a separate container. To customize which image is
being used to start that support container use method `withEtcdImage()`:

```java
ApiServerContainer<?> container = new ApiServerContainer<>().withEtcdImage(DockerImageName.parse("my-registry.com/etcd:.4.13-0"));
```

### `sshd` and `nginx` image for webhook testing

Testing dynamic admission control webhooks requires support containers with `nginx` and `sshd`. To customize which
images
are being used to start those support containers use the `withNginxImage()` and `withOpensshServerImage()` methods.

```java
ApiServerContainer<?> container = new ApiServerContainer()
        .withNginxImage(DockerImageName.parse("my-registry/nginx:1.23.3"))
        .withOpensshServerImage(DockerImageName.parse("my-registry/openssh-server:9.0_p1-r2-ls99"));
```

## Testing admission webhooks
You can use Kindcontainer to test your admission controllers.
* Make sure you start your webhooks before you start the Kindcontainer
* Start your webhooks without HTTPS/TLS
* Make sure your webhooks listen at `http://localhost:<port>`
* Register each webhook with `withAdmissionController()`

Example:
```java
ApiServerContainer<?> container = new ApiServerContainer().withAdmissionController(admission -> {
          admission.validating()    // use mutating() for a mutating admission controller
            .withNewWebhook("validating.kindcontainer.dajudge.com")
              .atPort(webhookPort)
              .withNewRule()
                .withApiGroups("")
                .withApiVersions("v1")
                .withOperations("CREATE", "UPDATE")
                .withResources("configmaps")
                .withScope("Namespaced")
              .endRule()
            .endWebhook()
            .build();
        })
```

# Examples

You can find examples in the [kindcontainer-examples](https://github.com/dajudge/kindcontainer-examples) repository.
