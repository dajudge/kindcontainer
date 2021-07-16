[![CI](https://github.com/dajudge/kafkaproxy/actions/workflows/build.yaml/badge.svg)](https://github.com/dajudge/kindcontainer/actions/workflows/build.yaml)
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
            <version>0.0.13</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

#### Gradle
Add the kindcontainer dependency:
```groovy
repositories {
    // Since 0.0.13 kindcontainer is on maven central
    mavenCentral()
}

dependencies {
    testImplementation "com.dajudge.kindcontainer:kindcontainer:0.0.13"
}
```
### Use in JUnit test
```java
public class SomeKubernetesTest {
    @ClassRule
    public static final KindContainer KUBE = new KindContainer();

    @Test
    public void test_something() {
        // Do something useful with the fabric8 client it returns!
        System.out.println(KUBE.client());
    }
}
```
