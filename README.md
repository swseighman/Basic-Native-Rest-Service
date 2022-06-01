## Building a Basic Spring Native REST Service 

As organizations modernize their applications and explore different cloud deployment options, they have a common desired outcome; to improve performance and reduce resource consumption. Ideally, these new services will lead to extensibility and preserve compatibility for existing applications while enhancing performance of these same applications.

GraalVM Enterprise offers features for accelerating the performance of existing Java applications and building of microservices all while reducing resources consumed. GraalVM is the foundation for new cloud native frameworks like Spring Boot, Micronaut and Quarkus.  In particular, Spring Native provides support for compiling Spring applications to native executables using the GraalVM native-image compiler.  Compared to the JIT-based applications, native images can enable low-cost, sustainable hosting for many types of workloads. These include microservices and function workloads, perfect for containers and Kubernetes.

In the following examples, we'll explore several options for building and deploying a Spring Boot native application, including:

* Using Java 17 (on Linux)
* Using Spring Boot Buildpacks support to generate a lightweight container with a native executable
* Creating your own custom container running a JAR or native executable
* Using the GraalVM native image Maven and Gradle plugins support to generate a native executable

Let's begin by cloning the demo repository:

```
$ git clone https://github.com/swseighman/Basic-Native-Rest-Service
```
Now change directory to the new project:

```
$ cd Basic-Rest-Service
```

To build the project, execute:
```
mvn package
```

>If you're using **Gradle**, execute the following command to build the application:
>```
>./gradlew build
>```

Let's build a container with a JAR version of the REST service.  The following command will use **Spring Boot’s Cloud Native Buildpacks** support to create a JAR-based application in a container.

**NOTE:** You'll need to install docker to create the container.

```
$ mvn spring-boot:build-image
... <snip>
[INFO]
[INFO] Successfully built image 'docker.io/library/rest-service-demo:0.0.1-SNAPSHOT'
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
$ docker run --rm -p 8080:8080 rest-service-demo:0.0.1-SNAPSHOT
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

The container/app started in approximately **2800ms**.

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
[INFO]     [creator]           docker.io/library/rest-service-demo:0.0.1-SNAPSHOT
[INFO]     [creator]     Adding cache layer 'paketo-buildpacks/graalvm:jdk'
[INFO]     [creator]     Adding cache layer 'paketo-buildpacks/native-image:native-image'
[INFO]
[INFO] Successfully built image 'docker.io/library/rest-service-demo:0.0.1-SNAPSHOT'
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  04:02 min
[INFO] Finished at: 2021-08-31T22:47:28-04:00
[INFO] ------------------------------------------------------------------------
```

>If you're using **Gradle**, execute the following command to build the container using Buildpacks:
>```
>./gradlew bootBuildImage
>```

Now let's run the container:

```
$ docker run --rm -p 8080:8080 rest-service-demo:0.0.1-SNAPSHOT
docker run -p 8080:8080 rest-service-demo:0.0.1-SNAPSHOT
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

The native executable container/app started in approximately **85ms**.

If you prefer `docker-compose`, you can create a `docker-compose.yml` at the root of the project with the following content:

```
version: '3.1'
services:
  rest-service:
    image: rest-service-demo:0.0.1-SNAPSHOT
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

#### Building a Native Image Executable

We can build a standalone native image executable using the `native` profile which we can add to our custom containers later in this lab. Let's build a native executable:

```
mvn package -Pnative
```
>If you're using **Gradle**, execute the following command to build the native image executable:
>```
>./gradlew nativeCompile
>```

The result will produce a native image executable.

>**NOTE:** Depending on your OS distribution, you may need to install some additional packages.  For example, with Oracle Linux/RHEL/Fedora distributions, I recommend installing the `Development Tools` to cover all of the dependencies you'll need to compile a native executable.  *You would also add this option in the appropriate Dockerfile.*

>```
>$ sudo dnf group install "Development Tools"
>```

To run the native executable application, execute the following:

```
$ target/rest-service-demo
<snip> ...
2022-04-04 11:27:58.076  INFO 27055 --- [           main] c.e.restservice.RestServiceApplication   : Started RestServiceApplication in 0.03 seconds (JVM running for 0.032)
```
The native executable started in approximately **30 ms**.

>If you're using **Gradle**, execute the following command:
>```
>build/native/nativeCompile/rest-service-demo
>```

#### Native Tests

Running the following command will build and run native tests:

```
$ mvn -Pnative test
```

>If you're using **Gradle**, execute the following command to build the native image executable:
>```
>./gradlew nativeTest
>```

You'll see output displayed similar to this example:

```
2021-09-21 13:34:51.478  INFO 22357 --- [           main] o.s.nativex.NativeListener               : This application is bootstrapped with code generated with Spring AOT

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.5.4)

2021-09-21 13:34:51.479  INFO 22357 --- [           main] c.e.restservice.GreetingControllerTests  : Starting GreetingControllerTests using Java 11.0.12 on sws-ryzen with PID 22357 (started by sseighma in /home/sseighma/code/Basic-Native-Rest-Service)
2021-09-21 13:34:51.479  INFO 22357 --- [           main] c.e.restservice.GreetingControllerTests  : No active profile set, falling back to default profiles: default
2021-09-21 13:34:51.528  INFO 22357 --- [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2021-09-21 13:34:51.528  INFO 22357 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2021-09-21 13:34:51.529  INFO 22357 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 0 ms
2021-09-21 13:34:51.530  INFO 22357 --- [           main] c.e.restservice.GreetingControllerTests  : Started GreetingControllerTests in 0.054 seconds (JVM running for 0.059)

MockHttpServletRequest:
      HTTP Method = GET
      Request URI = /greeting
       Parameters = {}
          Headers = []
             Body = null
    Session Attrs = {}

Handler:
             Type = com.example.restservice.GreetingController
           Method = com.example.restservice.GreetingController#greeting(String)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Content-Type:"application/json"]
     Content type = application/json
             Body = {"id":1,"content":"Hello, World!"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []

MockHttpServletRequest:
      HTTP Method = GET
      Request URI = /greeting
       Parameters = {name=[Spring Community]}
          Headers = []
             Body = null
    Session Attrs = {}

Handler:
             Type = com.example.restservice.GreetingController
           Method = com.example.restservice.GreetingController#greeting(String)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Content-Type:"application/json"]
     Content type = application/json
             Body = {"id":2,"content":"Hello, Spring Community!"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []
com.example.restservice.GreetingControllerTests > noParamGreetingShouldReturnDefaultMessage() SUCCESSFUL

com.example.restservice.GreetingControllerTests > paramGreetingShouldReturnTailoredMessage() SUCCESSFUL


Test run finished after 63 ms
[         2 containers found      ]
[         0 containers skipped    ]
[         2 containers started    ]
[         0 containers aborted    ]
[         2 containers successful ]
[         0 containers failed     ]
[         2 tests found           ]
[         0 tests skipped         ]
[         2 tests started         ]
[         0 tests aborted         ]
[         2 tests successful      ]
[         0 tests failed          ]

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:29 min
[INFO] Finished at: 2021-09-21T13:34:51-04:00
[INFO] ------------------------------------------------------------------------
```

#### Building a Static Native Image (x64 Linux only)

See [instructions](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/StaticImages/) for building and installing the required libraries.

After the process has been completed, copy `$ZLIB_DIR/libz.a` to `$GRAALVM_HOME/lib/static/linux-amd64/musl/`

Also add `x86_64-linux-musl-native/bin/x86_64-linux-musl-gcc` to your PATH.

Then execute:
```
mvn package -Pstatic
```

#### Container Options

Within this repository, there are a few examples of deploying applications in various container environments, from distroless to full OS images.  Choose the appropriate version for your use case and build the images.

For example, to build the JAR version:

```
$ docker build -f Dockerfile.jvm -t localhost/rest-service-demo:jvm .
```

```
$ docker run -i --rm -p 8080:8080 localhost/rest-service-demo:jvm
```

Browse to `localhost:8080/greeting`, where you should see:

```
{"id":1,"content":"Hello, World!"}
```

You can repeat these steps for each container option:

* Dockerfile.jvm
* Dockerfile.native
* Dockerfile.stage
* Dockerfile.distroless
* Dockerfile.static (x64 Linux only)

There is also a `build-containers.sh` script provided to build the container images.

```
$ docker images
localhost/rest-service-demo         distroless       a3b1cc5886b8  3 days ago     49 MB
localhost/rest-service-demo         native           2b0698c7b409  3 days ago    190 MB
localhost/rest-service-demo         jvm              c1f07f1e563e  3 days ago    605 MB
localhost/rest-service-demo         stage            dbae9b9333a7  3 days ago    190 MB
localhost/rest-service-demo         static           83bdf628adcd  3 days ago     76 MB
```

Also, you can choose to compress the native image executable using the [upx](https://upx.github.io/) utility which will reduce your container size but have little impact on startup performance.

For example:

```
$ upx -7 -k target/rest-service-demo
Ultimate Packer for eXecutables
                          Copyright (C) 1996 - 2020
UPX 3.96        Markus Oberhumer, Laszlo Molnar & John Reiser   Jan 23rd 2020

        File size         Ratio      Format      Name
   --------------------   ------   -----------   -----------
  84541616 ->  26604004   31.47%   linux/amd64   rest-service-demo

Packed 1 file.
```
Using `upx` we reduced the native image executable size by ~31% (from **81 M** to **26 M**):
```
-rwxrwxr-x 1 sseighma sseighma  26M Apr  4 10:44 rest-service-demo
-rwxrwxr-x 1 sseighma sseighma  81M Apr  4 10:44 rest-service-demo.~
```

Our native image container is now **140 MB** (versus the uncompressed version at **281 MB**):

```
$ docker images
localhost/rest-service-demo                       native           ff77aee72e96  8 seconds ago  140 MB
```