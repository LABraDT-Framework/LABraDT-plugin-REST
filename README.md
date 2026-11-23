# Rest

REST client for external services in the LABraDT Runtime (artifact `de.labradt:runtime-rest`). It
runs `EXTERNAL` / `REST` services: it sends the configured HTTP request (GET, POST, PUT, DELETE, query
parameters, JSON payload) and returns the response body as the service output.

## What is in here

| Path | What it is |
|---|---|
| `src/main/java/.../client/RestAdapter.java` | `IExternalServiceClient` for `REST`; uses the runtime core's shared `restClient` bean (`@Qualifier("restClient")`) |
| `src/main/java/.../client/HttpMethod.java` | The HTTP methods the adapter accepts |
| `src/test/` | `RestAdapterPublicUrlTest` (against httpbin.org) and `TestEnvironment` (reachability check) |

The packages are unchanged (`de.labradt.serviceexecution.runtime.external.client`), so the runtime
app finds the adapter through its normal component scan once the jar is on its classpath. The external
host (`ExternalApiRuntime`, `IExternalServiceClient`) and the shared HTTP client with its interceptors
(correlation id, logging, retry) stay in the runtime core.

## Build

The module builds against the runtime core (`runtime-external`, `core-api` and the runtime parent pom)
from the local Maven repository. There is no package registry yet, so install the runtime first:

```bash
# in the runtime project
mvn install -DskipTests

# then here
mvn install
```

The httpbin tests are tagged `integration`, so a normal `mvn test` leaves them out. `mvn test
-Pintegration` runs them; set `-Dtest.httpbin.url=...` for a local httpbin. Without a reachable httpbin
they are skipped with a reason.

## Use it with the runtime

Add the plugin to the dependencies of `service-manager-app/pom.xml` in the runtime project and rebuild
it:

```xml
<dependency>
    <groupId>de.labradt</groupId>
    <artifactId>runtime-rest</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

At startup the external host logs `Auto-registered client: REST → RestAdapter`.

