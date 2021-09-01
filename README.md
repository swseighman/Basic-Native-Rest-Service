## Building a Basic Spring Native REST Service 

As organizations modernize their applications and explore different cloud deployment options, they have a common desired outcome; to improve performance and reduce resource consumption. Ideally, these new services will lead to extensibility and preserve compatibility for existing applications while enhancing performance of these same applications.

GraalVM Enterprise offers features for accelerating the performance of existing Java applications and building of microservices all while reducing resources consumed. GraalVM is the foundation for new cloud native frameworks like Spring Boot, Micronaut and Quarkus.  In particular, Spring Native provides support for compiling Spring applications to native executables using the GraalVM native-image compiler.  Compared to the JIT-based applications, native images can enable low-cost, sustainable hosting for many types of workloads. These include microservices and function workloads, perfect for containers and Kubernetes.

In the following examples, we'll explore several options for building and deploying a Spring Boot native application, including:

* Using Spring Boot Buildpacks support to generate a lightweight container with a native executable
* Creating your own custom container running a JAR or native executable
* Using the GraalVM native image Maven plugin support to generate a native executable

Let's begin by cloning the demo repository:

```
$ git clone https://github.com/swseighman/Basic-Native-Rest-Service
```
Now change directory to the new project:

```
$ cd gs-rest-service/complete
```

First, let's build a container with a JAR version of the REST service.  The following command will use **Spring Boot’s Cloud Native Buildpacks** support to create a JAR-based application in a container:

```
$ mvn spring-boot:build-image
... <snip>
[INFO]
[INFO] Successfully built image 'docker.io/library/rest-service-complete:0.0.1-SNAPSHOT'
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  54.276 s
[INFO] Finished at: 2021-08-31T22:16:46-04:00
[INFO] ------------------------------------------------------------------------
```
**NOTE:** During the native compilation phase (throughout these examples), you will see many *WARNING: Could not register reflection metadata messages*, they are expected and will be removed in a future version.

Run the container and note the startup time:

```
$ docker run --rm -p 8080:8080 rest-service:0.0.1-SNAPSHOT
Setting Active Processor Count to 6
Calculating JVM memory based on 7397652K available memory
Calculated JVM Memory Configuration: -XX:MaxDirectMemorySize=10M -Xmx6802612K -XX:MaxMetaspaceSize=83039K -XX:ReservedCodeCacheSize=240M -Xss1M (Total Memory: 7397652K, Thread Count: 250, Loaded Class Count: 12247, Headroom: 0%)
Adding 129 container CA certificates to JVM truststore
Spring Cloud Bindings Enabled
Picked up JAVA_TOOL_OPTIONS: -Djava.security.properties=/layers/paketo-buildpacks_bellsoft-liberica/java-security-properties/java-security.properties -XX:+ExitOnOutOfMemoryError -XX:ActiveProcessorCount=6 -XX:MaxDirectMemorySize=10M -Xmx6802612K -XX:MaxMetaspaceSize=83039K -XX:ReservedCodeCacheSize=240M -Xss1M -Dorg.springframework.cloud.bindings.boot.enable=true

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.5.4)

2021-09-01 03:29:50.111  INFO 1 --- [           main] c.e.restservice.RestServiceApplication   : Starting RestServiceApplication v0.0.1-SNAPSHOT using Java 11.0.12 on f2f1a297ff62 with PID 1 (/workspace/BOOT-INF/classes started by cnb in /workspace)
2021-09-01 03:29:50.115  INFO 1 --- [           main] c.e.restservice.RestServiceApplication   : No active profile set, falling back to default profiles: default
2021-09-01 03:29:51.488  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2021-09-01 03:29:51.508  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2021-09-01 03:29:51.509  INFO 1 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.52]
2021-09-01 03:29:51.618  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2021-09-01 03:29:51.618  INFO 1 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 1406 ms
2021-09-01 03:29:52.139  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2021-09-01 03:29:52.152  INFO 1 --- [           main] c.e.restservice.RestServiceApplication   : Started RestServiceApplication in 2.769 seconds (JVM running for 3.544)
```

The container/app started in approximately **2800 m**s.

If you want to create a **native executable** in a container using `Buildpacks`, add the following to your `pom.xml`:

```
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <builder>paketobuildpacks/builder:tiny</builder>
            <env>
                <BP_NATIVE_IMAGE>true</BP_NATIVE_IMAGE>
            </env>
        </image>
    </configuration>
</plugin>
```
Then execute the same command:

```
$ mvn spring-boot:build-image
... <snip>
[INFO]     [creator]     *** Images (edbdf0ed8de1):
[INFO]     [creator]           docker.io/library/rest-service-complete:0.0.1-SNAPSHOT
[INFO]     [creator]     Adding cache layer 'paketo-buildpacks/graalvm:jdk'
[INFO]     [creator]     Adding cache layer 'paketo-buildpacks/native-image:native-image'
[INFO]
[INFO] Successfully built image 'docker.io/library/rest-service-complete:0.0.1-SNAPSHOT'
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  04:02 min
[INFO] Finished at: 2021-08-31T22:47:28-04:00
[INFO] ------------------------------------------------------------------------
```

Now let's run the container:

```
$ docker run --rm -p 8080:8080 rest-service:0.0.1-SNAPSHOT
docker run -p 8080:8080 rest-service-complete:0.0.1-SNAPSHOT
2021-09-01 03:43:13.817  INFO 1 --- [           main] o.s.nativex.NativeListener               : This application is bootstrapped with code generated with Spring AOT

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.5.4)

2021-09-01 03:43:13.821  INFO 1 --- [           main] c.e.restservice.RestServiceApplication   : Starting RestServiceApplication using Java 11.0.12 on afd146262820 with PID 1 (/workspace/com.example.restservice.RestServiceApplication started by cnb in /workspace)
2021-09-01 03:43:13.821  INFO 1 --- [           main] c.e.restservice.RestServiceApplication   : No active profile set, falling back to default profiles: default
2021-09-01 03:43:13.859  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2021-09-01 03:43:13.860  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2021-09-01 03:43:13.860  INFO 1 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.52]
2021-09-01 03:43:13.864  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2021-09-01 03:43:13.864  INFO 1 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 42 ms
2021-09-01 03:43:13.894  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2021-09-01 03:43:13.894  INFO 1 --- [           main] c.e.restservice.RestServiceApplication   : Started RestServiceApplication in 0.085 seconds (JVM running for 0.087)
```

**NOTE:** You can also use `podman`.

The native executable container/app started in approximately **85 m**s.

If you prefer `docker-compose`, you can create a `docker-compose.yml` at the root of the project with the following content:

```
version: '3.1'
services:
  rest-service:
    image: rest-service:0.0.1-SNAPSHOT
    ports:
      - "8080:8080"
```

And then execute:

```
$ docker-compose up
```

Browse to `localhost:8080/greeting`, where you should see:

```
{"id":1,"content":"Hello, World!"}
```

Or `curl http://localhost:8080/greeting`.

Next, let's build a native executable:

```
$ mvn -Pnative -DskipTests package
... <snip>
[rest-service-complete:20670]        image:  11,877.25 ms,  6.95 GB
[rest-service-complete:20670]        write:   6,180.24 ms,  6.95 GB
[rest-service-complete:20670]      [total]: 155,481.29 ms,  6.95 GB
# Printing build artifacts to: /Users/sseighma/code/spring/gs-rest-service/gs-rest-service/target/rest-service-complete.build_artifacts.txt
[INFO]
[INFO] --- spring-boot-maven-plugin:2.5.4:repackage (repackage) @ rest-service-complete ---
[INFO] Attaching repackaged archive /Users/sseighma/code/spring/gs-rest-service/gs-rest-service/target/rest-service-complete-0.0.1-SNAPSHOT-exec.jar with classifier exec
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:54 min
[INFO] Finished at: 2021-08-31T22:59:59-04:00
[INFO] ------------------------------------------------------------------------
```

To run the native executable application, execute the following:

```
$ target/gs-rest-service
2021-08-31 23:01:44.180  INFO 20826 --- [           main] o.s.nativex.NativeListener               : This application is bootstrapped with code generated with Spring AOT

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.5.4)

2021-08-31 23:01:44.181  INFO 20826 --- [           main] c.e.restservice.RestServiceApplication   : Starting RestServiceApplication v0.0.1-SNAPSHOT using Java 11.0.12 with PID 20826 (/Users/code/spring/gs-rest-service/gs-rest-service/target/rest-service-complete started by user in /Users/code/spring/gs-rest-service/gs-rest-service)
2021-08-31 23:01:44.181  INFO 20826 --- [           main] c.e.restservice.RestServiceApplication   : No active profile set, falling back to default profiles: default
2021-08-31 23:01:44.208  INFO 20826 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2021-08-31 23:01:44.208  INFO 20826 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2021-08-31 23:01:44.208  INFO 20826 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.52]
2021-08-31 23:01:44.213  INFO 20826 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2021-08-31 23:01:44.213  INFO 20826 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 31 ms
2021-08-31 23:01:44.226  INFO 20826 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2021-08-31 23:01:44.226  INFO 20826 --- [           main] c.e.restservice.RestServiceApplication   : Started RestServiceApplication in 0.065 seconds (JVM running for 0.066)
```
The native executable started in approximately **65 m**s.


#### Container Options

Within this repository, there are a few examples of deploying applications in various container environments, from distroless to full OS images.

Multi-stage builds

Depending on your Linux distribution, you may need to install some additional packages.  For example, in OL/RHEL/Fedora distributions, I recommend installing the `Development Tools` to cover all of the dependencies you'll need to compile a native executable.

```
$ sudo dnf group install "Development Tools"
```
