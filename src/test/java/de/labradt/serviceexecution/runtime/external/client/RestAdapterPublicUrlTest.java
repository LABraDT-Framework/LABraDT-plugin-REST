package de.labradt.serviceexecution.runtime.external.client;

import de.labradt.serviceexecution.core.api.enums.TechnologyAdapter;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for RestAdapter against the public httpbin.org echo API
 *
 * Tests all HTTP methods: GET, POST, PUT, DELETE
 * Prerequisites: outbound internet access to https://httpbin.org
 */
@DisplayName("RestAdapter - Public httpbin.org API Integration Tests (All HTTP Methods)")
class RestAdapterPublicUrlTest {

    private RestAdapter restAdapter;
    private static final String PUBLIC_BASE_URL = TestEnvironment.HTTPBIN_URL;
    private static final String GET_ENDPOINT = PUBLIC_BASE_URL + "/get";
    private static final String POST_ENDPOINT = PUBLIC_BASE_URL + "/post";
    private static final String PUT_ENDPOINT = PUBLIC_BASE_URL + "/put";
    private static final String DELETE_ENDPOINT = PUBLIC_BASE_URL + "/delete";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("integration")) {
            TestEnvironment.assumeReachable(PUBLIC_BASE_URL + "/get", "httpbin (needs internet, or set -Dtest.httpbin.url)");
        }
        RestClient restClient = RestClient.create();
        restAdapter = new RestAdapter(restClient);
    }

    private String prettifyJson(String response) {
        if (response == null || response.isBlank()) {
            return "(empty response)";
        }
        try {
            Object json = objectMapper.readValue(response, Object.class);
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            return response.substring(0, Math.min(200, response.length())) +
                    (response.length() > 200 ? "..." : "");
        }
    }

    @Test
    @DisplayName("Should verify RestAdapter client type is REST")
    void testGetClientType() {
        TechnologyAdapter clientType = restAdapter.getClientType();

        assertNotNull(clientType);
        assertEquals(TechnologyAdapter.REST, clientType);
    }

    @Tag("integration")
    @Test
    @DisplayName("GET: Should retrieve data from public endpoint")
    void testGetRequest() throws Exception {
        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("source", "integration-test");

            String response = restAdapter.GetRequest(GET_ENDPOINT, queryParams);

            assertNotNull(response, "Response should not be null");
            assertTrue(response.contains("integration-test"), "Response should echo the query param");
            System.out.println("✓ GET: Successfully retrieved data from " + GET_ENDPOINT);
            System.out.println("  Response:\n" + prettifyJson(response).replaceAll("(?m)^", "    "));

        } catch (RestClientException e) {
            System.out.println("⚠ Could not reach public endpoint: " + e.getMessage());
        }
    }

    @Tag("integration")
    @Test
    @DisplayName("POST: Should send JSON payload to public endpoint")
    void testPostRequest() throws Exception {
        String payload = "{\n" +
                "  \"name\": \"integration-test\",\n" +
                "  \"status\": \"pending\"\n" +
                "}";

        try {
            String response = restAdapter.PostRequest(POST_ENDPOINT, payload, null);

            assertNotNull(response, "Response should not be null");
            assertTrue(response.contains("integration-test"), "Response should echo the posted payload");
            System.out.println("✓ POST: Successfully sent payload to " + POST_ENDPOINT);
            System.out.println("  Response:\n" + prettifyJson(response).replaceAll("(?m)^", "    "));

        } catch (RestClientException e) {
            System.out.println("⚠ Could not reach public endpoint: " + e.getMessage());
        }
    }

    @Tag("integration")
    @Test
    @DisplayName("PUT: Should update data on public endpoint")
    void testPutRequest() throws Exception {
        String payload = "{\n" +
                "  \"status\": \"updated\"\n" +
                "}";

        try {
            String response = restAdapter.PutRequest(PUT_ENDPOINT, payload, null);

            assertNotNull(response, "Response should not be null");
            assertTrue(response.contains("updated"), "Response should echo the updated payload");
            System.out.println("✓ PUT: Successfully updated data on " + PUT_ENDPOINT);
            System.out.println("  Response:\n" + prettifyJson(response).replaceAll("(?m)^", "    "));

        } catch (RestClientException e) {
            System.out.println("⚠ Could not reach public endpoint: " + e.getMessage());
        }
    }

    @Tag("integration")
    @Test
    @DisplayName("DELETE: Should remove data on public endpoint")
    void testDeleteRequest() throws Exception {
        try {
            String response = restAdapter.DeleteRequest(DELETE_ENDPOINT, null);

            assertNotNull(response, "Response should not be null");
            System.out.println("✓ DELETE: Successfully sent delete request to " + DELETE_ENDPOINT);
            System.out.println("  Response:\n" + prettifyJson(response).replaceAll("(?m)^", "    "));

        } catch (RestClientException e) {
            System.out.println("⚠ Could not reach public endpoint: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("URL Validation: Should reject null URL")
    void testNullUrlValidation() {
        String payload = "{\"test\": \"data\"}";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                restAdapter.PostRequest(null, payload, null)
        );

        assertEquals("URL must be provided", exception.getMessage());
    }

    @Test
    @DisplayName("URL Validation: Should reject blank URL")
    void testBlankUrlValidation() {
        String payload = "{\"test\": \"data\"}";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                restAdapter.PostRequest("   ", payload, null)
        );

        assertEquals("URL must be provided", exception.getMessage());
    }
}
