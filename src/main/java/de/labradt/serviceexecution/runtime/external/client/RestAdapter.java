package de.labradt.serviceexecution.runtime.external.client;

import de.labradt.serviceexecution.core.api.enums.TechnologyAdapter;
import de.labradt.serviceexecution.runtime.external.client.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RestAdapter implements IExternalServiceClient {

    private final RestClient restClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public RestAdapter(@Qualifier("restClient") RestClient restClient) {
        this.restClient = restClient;
        log.info("RestAdapter initialized with RestClient");
    }

    @Override
    public TechnologyAdapter getClientType() {
        return TechnologyAdapter.REST;
    }

    /**
     * Parse query parameters from JSON string into Map<String, String>
     * Expects JSON format: {"key1": "value1", "key2": "value2"}
     */
    private Map<String, String> parseQueryParams(String queryParamsJson) {
        if (queryParamsJson == null || queryParamsJson.isBlank()) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> parsed = objectMapper.readValue(queryParamsJson, Map.class);
            log.debug("Successfully parsed query parameters: {}", parsed.keySet());
            return parsed;
        } catch (Exception e) {
            log.warn("Failed to parse query parameters from JSON: {}", queryParamsJson, e);
            return null;
        }
    }

    /**
     * SendRequest: Parses this adapter's REST-specific keys (url, httpMethod, payload,
     * queryParams) out of the generic args map, then routes based on HTTP method.
     * This is the main entry point - all external calls go through here.
     */
    @Override
    public String SendRequest(Map<String, String> args) throws Exception {
        if (args == null) {
            throw new IllegalArgumentException("args must not be null");
        }

        String targetUrl = args.get("url");
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("URL must be provided in request arguments under the key 'url'");
        }

        HttpMethod httpMethod = HttpMethod.POST;
        String httpMethodStr = args.get("httpMethod");
        if (httpMethodStr != null && !httpMethodStr.isBlank()) {
            try {
                httpMethod = HttpMethod.valueOf(httpMethodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid HTTP method '{}', defaulting to POST", httpMethodStr);
            }
        }

        String payload = args.getOrDefault("payload", "{}");

        Map<String, String> queryParams = null;
        String queryParamsStr = args.get("queryParams");
        if (queryParamsStr != null && !queryParamsStr.isBlank()) {
            queryParams = parseQueryParams(queryParamsStr);
        }


        log.debug("RestAdapter routing {} request to {}", httpMethod, targetUrl);

        return switch(httpMethod) {
            case GET -> GetRequest(targetUrl, queryParams);
            case POST -> PostRequest(targetUrl, payload, queryParams);
            case PUT -> PutRequest(targetUrl, payload, queryParams);
            case DELETE -> DeleteRequest(targetUrl, queryParams);
            case PATCH, HEAD, OPTIONS ->
                throw new UnsupportedOperationException("HTTP method " + httpMethod + " not yet implemented");
        };
    }

    /**
     * Build query string from parameter map
     */
    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return "?" + params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /**
     * POST: Send a request with optional payload and query parameters
     * If payload is null/blank, it will not be included in the request
     * If queryParams is null/empty, no query string will be appended
     */
    public String PostRequest(String targetUrl, String payload, Map<String, String> queryParams) throws Exception {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("URL must be provided");
        }

        String urlWithParams = targetUrl + buildQueryString(queryParams);
        log.debug("Sending RestAdapter POST request to: {}", urlWithParams);

        try {
            long startTime = System.currentTimeMillis();

            var postSpec = restClient.post()
                    .uri(urlWithParams)
                    .header("Content-Type", "application/json");

            String responseBody;
            if (payload != null && !payload.isBlank()) {
                responseBody = postSpec.body(payload)
                        .retrieve()
                        .body(String.class);
            } else {
                responseBody = postSpec.retrieve()
                        .body(String.class);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Rest Adapter POST response received | duration={}ms", duration);

            return responseBody;

        } catch (RestClientException e) {
            log.error("RestAdapter POST request failed: {}", e.getMessage(), e);
            throw new Exception("Failed to send RestAdapter POST request: " + e.getMessage(), e);
        }
    }

    /**
     * GET: Retrieve data with optional query parameters
     * If queryParams is null/empty, no query string will be appended
     */
    public String GetRequest(String targetUrl, Map<String, String> queryParams) throws Exception {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("URL must be provided");
        }

        String urlWithParams = targetUrl + buildQueryString(queryParams);
        log.debug("Sending RestAdapter GET request to: {}", urlWithParams);

        try {
            long startTime = System.currentTimeMillis();

            String responseBody = restClient.get()
                    .uri(urlWithParams)
                    .retrieve()
                    .body(String.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Rest Adapter GET response received | duration={}ms", duration);

            return responseBody;

        } catch (RestClientException e) {
            log.error("RestAdapter GET request failed: {}", e.getMessage(), e);
            throw new Exception("Failed to send RestAdapter GET request: " + e.getMessage(), e);
        }
    }

    /**
     * PUT: Update with optional payload and query parameters
     * If payload is null/blank, it will not be included in the request
     * If queryParams is null/empty, no query string will be appended
     */
    public String PutRequest(String targetUrl, String payload, Map<String, String> queryParams) throws Exception {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("URL must be provided");
        }

        String urlWithParams = targetUrl + buildQueryString(queryParams);
        log.debug("Sending RestAdapter PUT request to: {}", urlWithParams);

        try {
            long startTime = System.currentTimeMillis();

            var putSpec = restClient.put()
                    .uri(urlWithParams);

            String responseBody;
            if (payload != null && !payload.isBlank()) {
                responseBody = putSpec.header("Content-Type", "application/json")
                        .body(payload)
                        .retrieve()
                        .body(String.class);
            } else {
                responseBody = putSpec.retrieve()
                        .body(String.class);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Rest Adapter PUT response received | duration={}ms", duration);

            return responseBody;

        } catch (RestClientException e) {
            log.error("RestAdapter PUT request failed: {}", e.getMessage(), e);
            throw new Exception("Failed to send RestAdapter PUT request: " + e.getMessage(), e);
        }
    }

    /**
     * DELETE: Remove a resource with optional query parameters
     * If queryParams is null/empty, no query string will be appended
     */
    public String DeleteRequest(String targetUrl, Map<String, String> queryParams) throws Exception {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("URL must be provided");
        }

        String urlWithParams = targetUrl + buildQueryString(queryParams);
        log.debug("Sending RestAdapter DELETE request to: {}", urlWithParams);

        try {
            long startTime = System.currentTimeMillis();

            String responseBody = restClient.delete()
                    .uri(urlWithParams)
                    .retrieve()
                    .body(String.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Rest Adapter DELETE response received | duration={}ms", duration);

            return responseBody;

        } catch (RestClientException e) {
            log.error("RestAdapter DELETE request failed: {}", e.getMessage(), e);
            throw new Exception("Failed to send RestAdapter DELETE request: " + e.getMessage(), e);
        }
    }
}
