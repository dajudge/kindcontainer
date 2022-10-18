[![CI](https://github.com/dajudge/kindcontainer/actions/workflows/build.yaml/badge.svg)](https://github.com/dajudge/kindcontainer/actions/workflows/build.yaml)
[![Maven central](https://img.shields.io/maven-central/v/com.dajudge.kindcontainer/kindcontainer)](https://search.maven.org/artifact/com.dajudge.kindcontainer/kindcontainer)

Kindcontainer
---
A Java-based [Testcontainers](https://www.testcontainers.org/) container implementation that provides ephemeral
Kubernetes clusters for integration testing.

<!-- TOC -->
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
  * [Using custom docker images](#using-custom-docker-images)
  * [Using the `kubectl` and `helm` fluent APIs](#using-the-kubectl-and-helm-fluent-apis)
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
            <version>1.3.1</version>
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
    testImplementation "com.dajudge.kindcontainer:kindcontainer:1.3.1"
}
```

## Use in JUnit 4 test

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

Look [here](src/test/java/com/dajudge/kindcontainer/readme/SomeKindTest.java) for the reference test.

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

Look [here](src/test/java/com/dajudge/kindcontainer/readme/SomeK3sTest.java) for the reference test.

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

Look [here](src/test/java/com/dajudge/kindcontainer/readme/SomeApiServerTest.java) for the reference test.

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

## Using custom docker images

In some environments it might be necessary to use custom docker images for the Kubernetes containers. This can be
by suffixing the kubernetes version you want to run with a call to `withImage()` like this:

```java
KindContainer<?> container=new KindContainer<>(KindContainerVersion.VERSION_1_24_1.withImage("my-registry.com/kind:1.24.1"));
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

# Examples

You can find examples in the [kindcontainer-examples](https://github.com/dajudge/kindcontainer-examples) repository.
