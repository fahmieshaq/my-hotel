package com.maz.my_hotel.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RequestIdFilterTest {

    /**
     * Verifies that each incoming HTTP request receives
     * a newly generated, valid UUID as its request ID.
     */
    @Test
    void doFilterInternal_generateUniqueRequestId_success() throws Exception {
        // Arrange: Create the filter being tested.
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate two independent HTTP requests.
        // JUnit + MockHttpServletRequest/Response: Unit-test the filter's logic
        // We're testing the filter in isolation, one step at a time.
        // You could setup request and response body but why don't we need a request or response body?
        // Because our RequestIdFilter doesn't process HTTP bodies.
        //        It only needs to:
        //        - Generate a request ID.
        //        - Read an optional correlation ID from the request headers.
        //        - Write both identifiers to the response headers.
        //        - Manage MDC.
        //        Even an HTTP request without a body is perfectly valid.
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();

        // Capture the HTTP responses without starting Tomcat.
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        // Act: Execute the filter once for each request.
        // The HTTP request is triggered (simulated) when you call filter.doFilter(), not when you create the mock objects.
        //
        // doFilter() is the public entry point invoked by the servlet container.
        // OncePerRequestFilter.doFilter() handles the framework behavior, including
        // its already-filtered checks and dispatch handling, before calling doFilterInternal().
        // In this unit test, the controller never executes; new MockFilterChain() provides a simulated filter chain
        // without a real controller or servlet handling the request. We're testing only RequestIdFilter, not the complete Hotel API.
        // filter.doFilter() directly invokes the filter using simulated HTTP objects. It doesn't send an actual
        // network request. That's why this is a unit test rather than a full HTTP integration test.
        filter.doFilter(request1, response1, new MockFilterChain());
        filter.doFilter(request2, response2, new MockFilterChain());

        // Retrieve the generated request IDs
        String requestId1 = response1.getHeader("X-Request-ID");
        String requestId2 = response2.getHeader("X-Request-ID");

        // Assert: Both responses must contain a request ID
        assertNotNull(requestId1);
        assertNotNull(requestId2);

        // Assert: Both IDs must be valid UUIDs.
        assertEquals(requestId1, UUID.fromString(requestId1).toString());
        assertEquals(requestId2, UUID.fromString(requestId2).toString());

        // Assert: Independent requests must have different IDs
        assertNotEquals(requestId1, requestId2);
    }

    @Test
    void doFilterInternal_clientPassesValidCorrelationId_success() throws Exception {
        // Arrange: Create the filter being tested.
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate an incoming HTTP request.
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Simulate the outgoing HTTP response.
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate the remaining servlet filter chain.
        MockFilterChain filterChain = new MockFilterChain();

        // Simulate a client providing an existing correlation ID.
        String expectedCorrelationId = "BOOKING-123";

        request.addHeader("X-Correlation-ID", expectedCorrelationId);

        // Act: Execute the filter using the simulated HTTP objects.
        filter.doFilter(request, response, filterChain);

        // Assert: The response must preserve the supplied correlation ID
        assertEquals(expectedCorrelationId, response.getHeader("X-Correlation-ID"));

        String requestId = response.getHeader("X-Request-ID");
        assertNotNull(requestId);
        assertEquals(requestId, UUID.fromString(requestId).toString());

        // Assert: The request ID must differ from the correlation ID
        assertNotEquals(expectedCorrelationId, requestId);
    }

    /**
     * Verifies that when the client does not supply a correlation ID,
     * the filter generates a valid UUID that is different from
     * the automatically generated request ID.
     */
    @Test
    void doFilterInternal_clientPassesNoCorrelationId_success() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate an HTTP request without a correlation ID header.
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Capture the HTTP response generated by the filter.
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate the remaining servlet filter chain.
        MockFilterChain filterChain = new MockFilterChain();

        // Act: Execute the filter.
        filter.doFilter(request, response, filterChain);

        // Retrieve both generated identifiers from the response headers.
        String requestId = response.getHeader("X-Request-ID");
        String correlationId = response.getHeader("X-Correlation-ID");

        // Assert: Both identifiers must exist.
        assertNotNull(requestId);
        assertNotNull(correlationId);

        // Assert: The correlation ID must be a valid UUID.
        assertEquals(
                correlationId,
                UUID.fromString(correlationId).toString()
        );

        // Assert: The correlation ID must be independent
        // of the request ID, not a copy of it.
        assertNotEquals(requestId, correlationId);
    }

    /**
     * Verifies that when the client supplies a blank correlation ID,
     * the filter rejects it and generates a new valid UUID.
     *
     * A blank correlation ID must never be propagated to logs
     * or returned unchanged in the HTTP response.
     */
    @Test
    void doFilterInternal_clientPassesBlankCorrelationId_success() throws Exception {

        // Arrange: Create the filter being tested.
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate an incoming HTTP request.
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Capture the outgoing HTTP response.
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate the remaining servlet filter chain.
        MockFilterChain filterChain = new MockFilterChain();

        // Simulate a client supplying a blank correlation ID.
        request.addHeader("X-Correlation-ID", "   ");

        // Act: Execute the filter.
        filter.doFilter(request, response, filterChain);

        // Retrieve the correlation ID returned by the filter.
        String correlationId = response.getHeader("X-Correlation-ID");

        // Assert: The filter must generate a correlation ID.
        assertNotNull(correlationId);

        // Assert: The generated correlation ID must be a valid UUID.
        assertEquals(
                correlationId,
                UUID.fromString(correlationId).toString()
        );

        // Assert: The invalid incoming value must not be preserved.
        assertNotEquals("   ", correlationId);

        // Assert: The correlation ID must remain independent
        // of the generated request ID.
        assertNotEquals(
                response.getHeader("X-Request-ID"),
                correlationId
        );
    }

    /**
     * Verifies that a correlation ID exceeding the maximum
     * allowed length of 128 characters is rejected.
     *
     * The filter must generate a new valid UUID instead of
     * propagating an excessively long client-supplied value.
     */
    @Test
    void doFilterInternal_clientPassesOversizedCorrelationId_success() throws Exception {

        // Arrange: Create the filter being tested.
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate an incoming HTTP request.
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Capture the outgoing HTTP response.
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate the remaining servlet filter chain.
        MockFilterChain filterChain = new MockFilterChain();

        // Generate an invalid correlation ID containing 129 characters.
        // This exceeds our configured maximum of 128 characters.
        String invalidCorrelationId = "A".repeat(129);

        // Simulate the client supplying the oversized correlation ID.
        request.addHeader("X-Correlation-ID", invalidCorrelationId);

        // Act: Execute the filter.
        filter.doFilter(request, response, filterChain);

        // Retrieve the correlation ID returned by the filter.
        String correlationId = response.getHeader("X-Correlation-ID");

        // Assert: A correlation ID must be returned.
        assertNotNull(correlationId);

        // Assert: The oversized value must not be preserved.
        assertNotEquals(invalidCorrelationId, correlationId);

        // Assert: The replacement must be a valid UUID.
        assertEquals(
                correlationId,
                UUID.fromString(correlationId).toString()
        );

        // Assert: The correlation ID must remain independent
        // of the request ID.
        assertNotEquals(
                response.getHeader("X-Request-ID"),
                correlationId
        );
    }

    /**
     * Verifies that a client-supplied correlation ID containing
     * disallowed characters is rejected.
     *
     * The filter must generate a new UUID rather than
     * preserving the invalid correlation ID.
     */
    @Test
    void doFilterInternal_clientPassesUnsafeCorrelationId_success() throws Exception {

        // Arrange: Create the filter being tested.
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate an incoming HTTP request.
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Capture the outgoing HTTP response.
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate the remaining servlet filter chain.
        MockFilterChain filterChain = new MockFilterChain();

        // This correlation ID contains an invalid character: @
        String invalidCorrelationId = "BOOKING@123";

        // Simulate the client supplying the invalid correlation ID.
        request.addHeader("X-Correlation-ID", invalidCorrelationId);

        // Act: Execute the filter.
        filter.doFilter(request, response, filterChain);

        // Retrieve the correlation ID returned by the filter.
        String correlationId = response.getHeader("X-Correlation-ID");

        // Assert: The filter must return a correlation ID.
        assertNotNull(correlationId);

        // Assert: The invalid client-supplied value must be rejected.
        assertNotEquals(invalidCorrelationId, correlationId);

        // Assert: The replacement must be a valid UUID.
        assertEquals(
                correlationId,
                UUID.fromString(correlationId).toString()
        );

        // Assert: The correlation ID must remain independent
        // of the request ID.
        assertNotEquals(
                response.getHeader("X-Request-ID"),
                correlationId
        );
    }

    /**
     * Verifies that the filter removes both requestId and correlationId
     * from MDC after completing request processing.
     *
     * This prevents request-specific logging context from leaking
     * into subsequent requests handled by the same thread.
     *
     * Tests that MDC stores request information while an HTTP request
     * is being processed and removes it when processing finishes.
     *
     * MDC (Mapped Diagnostic Context) is like a temporary dictionary
     * containing key-value pairs for logging.
     *
     * Example:
     *   requestId     -> "abc-123"
     *   correlationId -> "booking-456"
     *
     * Each thread normally has its own MDC dictionary.
     *
     * Logback can read these values and automatically include them
     * in log messages without us manually passing the IDs to every logger.
     *
     * Because server threads are reused for different HTTP requests,
     * we must remove these values after each request finishes.
     */
    @Test
    void doFilterInternal_clearsMdcAfterRequest_success() throws Exception {

        // Arrange: Create the filter being tested.
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate an incoming HTTP request.
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Capture the outgoing HTTP response.
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate the remaining servlet filter chain.
        MockFilterChain filterChain = new MockFilterChain();

        // Act: Execute the filter.
        filter.doFilter(request, response, filterChain);

        // Assert: The filter must have generated both identifiers.
        assertNotNull(response.getHeader("X-Request-ID"));
        assertNotNull(response.getHeader("X-Correlation-ID"));

        // Assert: Both MDC entries must be removed after processing.
        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("correlationId"));
    }

    /**
     * Verifies that MDC is cleaned up even when request processing
     * throws an exception.
     *
     * MDC is a temporary key-value dictionary attached to the
     * current thread. It stores logging information such as:
     *
     *   requestId     -> "abc-123"
     *   correlationId -> "booking-456"
     *
     * If request processing fails, the filter's finally block
     * must still remove these values.
     */
    @Test
    void doFilterInternal_clearsMdcWhenExceptionOccurs_success() throws Exception {

        // Arrange: Create the filter being tested.
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate an incoming HTTP request.
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Capture the outgoing HTTP response.
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate a filter chain that fails during request processing.
        FilterChain filterChain = (req, res) -> {

            // Verify that both IDs are available in MDC
            // before the exception occurs.
            assertNotNull(MDC.get("requestId"));
            assertNotNull(MDC.get("correlationId"));

            // Simulate a failure somewhere downstream,
            // such as in another filter or a controller.
            throw new ServletException("Simulated request failure");
        };

        // Act & Assert: Verify that the exception propagates
        // out of our filter rather than being swallowed.
        ServletException exception = assertThrows(
                ServletException.class,
                () -> filter.doFilter(request, response, filterChain)
        );

        // Verify that the original exception message is preserved.
        assertEquals("Simulated request failure", exception.getMessage());

        // Assert: Both IDs must have been generated.
        assertNotNull(response.getHeader("X-Request-ID"));
        assertNotNull(response.getHeader("X-Correlation-ID"));

        // Assert: Despite the exception, MDC must be cleared.
        // The finally block in RequestIdFilter is responsible for this.
        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("correlationId"));
    }

    /**
     * Verifies that a client-supplied correlation ID containing
     * exactly 128 valid characters is accepted.
     *
     * Our maximum allowed length is 128 characters.
     * Therefore:
     *   128 characters -> accepted
     *   129 characters -> rejected
     *
     * This test protects against off-by-one errors.
     */
    @Test
    void doFilterInternal_clientPassesMaxLengthCorrelationId_success() throws Exception {

        // Arrange: Create the filter being tested.
        RequestIdFilter filter = new RequestIdFilter();

        // Simulate an incoming HTTP request.
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Capture the outgoing HTTP response.
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate the remaining servlet filter chain.
        MockFilterChain filterChain = new MockFilterChain();

        // Generate a valid correlation ID containing exactly 128 characters.
        String expectedCorrelationId = "A".repeat(128);

        // Simulate the client providing this correlation ID.
        request.addHeader("X-Correlation-ID", expectedCorrelationId);

        // Act: Execute the filter.
        filter.doFilter(request, response, filterChain);

        // Assert: The filter must preserve the original correlation ID.
        assertEquals(
                expectedCorrelationId,
                response.getHeader("X-Correlation-ID")
        );

        // Assert: The request ID must still be generated independently.
        String requestId = response.getHeader("X-Request-ID");

        assertNotNull(requestId);
        assertNotEquals(expectedCorrelationId, requestId);
    }
}
