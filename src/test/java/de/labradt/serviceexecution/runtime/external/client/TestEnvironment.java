package de.labradt.serviceexecution.runtime.external.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Reachability checks for the tests tagged {@code integration}: when the service is not
 * reachable they are skipped with a clear reason. Normal {@code mvn test} excludes the tag;
 * {@code mvn test -Pintegration} runs it. Addresses come from system properties.
 */
final class TestEnvironment {

    /** Public HTTP echo service. Override: -Dtest.httpbin.url=... */
    static final String HTTPBIN_URL = System.getProperty("test.httpbin.url", "https://httpbin.org");

    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private TestEnvironment() {
    }

    static void assumeReachable(String probeUrl, String what) {
        assumeTrue(CACHE.computeIfAbsent(probeUrl, TestEnvironment::answers200),
                what + " not reachable at " + probeUrl + " - skipped");
    }

    private static boolean answers200(String url) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
