kindcontainer
---
A Java-based [testcontainers.org](https://www.testcontainers.org/) container implementation that uses 
[Kubernetes in Docker](https://github.com/kubernetes-sigs/kind) (KIND) to provide ephemeral Kubernetes
clusters for unit/integration testing.

## Usage
### Add dependency
First you need to add the kindcontainer dependency to your build. Kindcontainer is available as a [bintray repository](https://bintray.com/dajudge/kindcontainer/kindcontainer).
#### Maven
Add the bintray repo and the kindcontainer dependency:
```xml
<project>
    <!-- kindcontainer is not on maven central, yet -->
    <repositories>
        <repository>
            <id>bintray-kindcontainer</id>
            <name>The kindcontainer bintray repo</name>
            <url>https://dl.bintray.com/dajudge/kindcontainer</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.dajudge.kindcontainer</groupId>
            <artifactId>kindcontainer</artifactId>
            <version>${kindcontainer.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

#### Gradle
Add the bintray repo and the kindcontainer dependency:
```groovy
repositories {
    // kindcontainer is not on Maven central, yet
    maven {
        url "https://dl.bintray.com/dajudge/kindcontainer"
    }
    // But it has some transitive dependencies there
    mavenCentral()
}

dependencies {
    testImplementation "com.dajudge.kindcontainer:kindcontainer:${kindcontainerVersion}"
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