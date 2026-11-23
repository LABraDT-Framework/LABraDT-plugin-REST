package de.labradt.serviceexecution.runtime.external.client;

/**
 * HttpMethod: Standard HTTP request methods
 * 
 * Provides type-safety for HTTP method routing in adapter
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS
}
