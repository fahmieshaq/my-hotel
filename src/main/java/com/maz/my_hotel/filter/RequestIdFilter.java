package com.maz.my_hotel.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter responsible for managing request and correlation IDs.
 *
 * This filter will eventually:
 * 1. Generate a unique request ID for each HTTP request.
 * 2. Read or generate a correlation ID.
 * 3. Store both identifiers in SLF4J MDC.
 * 4. Make the identifiers available in application logs.
 * 5. Clean up MDC after request processing.
 */
@Component // tells Spring to discover this class during component scanning and create a Spring-managed bean.
public class RequestIdFilter extends OncePerRequestFilter {
    // Standardized header name used to pass the correlation ID
    // between clients, APIs, and downstream services.
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    // HTTP response header containing the unique request identifier.
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    // Maximum allowed length to prevent excessively large identifiers.
    private static final int MAX_CORRELATION_ID_LENGTH = 128;

    /**
     * Executes when an HTTP request passes through this filter.
     *
     * @param request     Incoming HTTP request.
     * @param response    Outgoing HTTP response.
     * @param filterChain Remaining filters and request processing.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Generate a unique ID for this individual HTTP request.
        // This helps us identify and troubleshoot a specific request.
        // we need to make it available to our logging framework.
        String requestId = UUID.randomUUID().toString();

        // Read the correlation ID supplied by the HTTP client.
        // This allows related requests to share the same identifier.
        // We're checking whether the client or another service has already assigned a correlation
        // The frontend deliberately sends the same correlation ID with each related request.
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        // Generate a new correlation ID when the supplied value
        // is missing, blank, too long, or contains unsafe characters.
        if (correlationId == null
                || correlationId.isBlank()
                || correlationId.length() > MAX_CORRELATION_ID_LENGTH
                || !correlationId.matches("[a-zA-Z0-9_-]+")) {
            correlationId = UUID.randomUUID().toString();
        }

        try {
            // Every request gets both request ID and correlation ID

            // Store the request ID in the current thread's MDC.
            // Logback can retrieve this value when formatting logs.
            MDC.put("requestId", requestId);
            // Store the correlation ID so Logback can include it in logs.
            MDC.put("correlationId", correlationId);

            // Return the generated request ID to the HTTP client.
            // Useful when a client reports an error and provides its request ID.
            // Expose the identifier in the HTTP response header
            response.setHeader(REQUEST_ID_HEADER, requestId);

            // Return the correlation ID to the client.
            // The client can reuse this ID in subsequent related requests.
            // Expose the identifier in the HTTP response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // A Filter Chain is a sequence of servlet filters that can inspect, modify, or control an HTTP request before it reaches Spring MVC.
            filterChain.doFilter(request, response);
        } finally {
            // Remove the request ID from MDC after processing.
            // Tomcat reuses worker threads, so cleanup prevents
            // the next request from inheriting this request's ID.
            MDC.remove("requestId");
            // Prevent the next request on this thread from inheriting this ID.
            MDC.remove("correlationId");
        }

    }
}
