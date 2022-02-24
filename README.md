[![CI](https://github.com/dajudge/kindcontainer/actions/workflows/build.yaml/badge.svg)](https://github.com/dajudge/kindcontainer/actions/workflows/build.yaml)
[![Maven central](https://img.shields.io/maven-central/v/com.dajudge.kindcontainer/kindcontainer)](https://search.maven.org/artifact/com.dajudge.kindcontainer/kindcontainer)

kindcontainer
---
A Java-based [testcontainers.org](https://www.testcontainers.org/) container implementation that uses 
[Kubernetes in Docker](https://github.com/kubernetes-sigs/kind) (KIND) to provide ephemeral Kubernetes
clusters for unit/integration testing.

## Usage
### Add dependency
First you need to add the kindcontainer dependency to your build. Kindcontainer is available on maven central.
#### Maven
Add the kindcontainer dependency:
```xml
<project>
    <dependencies>
        <dependency>
            <groupId>com.dajudge.kindcontainer</groupId>
            <artifactId>kindcontainer</artifactId>
            <version>1.0.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

#### Gradle
Add the kindcontainer dependency:
```groovy
repositories {
    mavenCentral()
}

dependencies {
    testImplementation "com.dajudge.kindcontainer:kindcontainer:1.0.0"
}
```
### Use in JUnit test
```java
public class SomeKubernetesTest {
    @ClassRule
    public static final KindContainer<?> KUBE = new KindContainer<>();

    @Test
    public void verify_node_is_present() {
        // Create a fabric8 client and use it!
        try(final KubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(KUBE.getKubeconfig()))) {
            assertEquals(1, client.nodes().list().getItems().size());
        }
    }
}
```
Look [here](src/test/java/com/dajudge/kindcontainer/readme/SomeKubernetesTest.java) for the reference test.

### API-Server-only testing
If you don't need a full-fledged Kubernetes distribution for your testing, using the `ApiServerContainer`
might be an option for you that shaves off a lot of the startup overhead of the `KindContainer`. The
`ApiServerContainer` only starts an etcd instance and a Kubernetes API-Server, can be more than enough
e.g. if all you want to test is if your custom operator handles it's CRDs properly or creates the required
objects in the control plane.

```java
public class SomeControlPlaneTest {
    @ClassRule
    public static final ApiServerContainer<?> KUBE = new ApiServerContainer<>();

    @Test
    public void verify_no_node_is_present() {
        // Create a fabric8 client and use it!
        try (final KubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(KUBE.getKubeconfig()))) {
            assertTrue(client.nodes().list().getItems().isEmpty());
        }
    }
}
```
Look [here](src/test/java/com/dajudge/kindcontainer/readme/SomeControlPlaneTest.java) for the reference test.