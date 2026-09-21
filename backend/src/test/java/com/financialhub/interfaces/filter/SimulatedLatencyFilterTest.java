package com.financialhub.interfaces.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedLatencyFilterTest {

    @Test
    void shouldSkipWhenDisabled() throws Exception {
        SimulatedLatencyFilter filter = new SimulatedLatencyFilter(0);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilter(request, response, chain);

        assertNull(response.getHeader(SimulatedLatencyFilter.HEADER_NAME));
        assertTrue(called.get());
    }

    @Test
    void shouldSleepAndExposeHeader() throws Exception {
        SimulatedLatencyFilter filter = new SimulatedLatencyFilter(5);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();
        FilterChain chain = (req, res) -> called.set(true);

        long started = System.nanoTime();
        filter.doFilter(request, response, chain);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        assertEquals("5", response.getHeader(SimulatedLatencyFilter.HEADER_NAME));
        assertTrue(elapsedMs >= 5);
        assertTrue(called.get());
    }

    @Test
    void shouldSkipActuator() throws Exception {
        SimulatedLatencyFilter filter = new SimulatedLatencyFilter(50);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilter(request, response, chain);

        assertNull(response.getHeader(SimulatedLatencyFilter.HEADER_NAME));
        assertTrue(called.get());
    }
}
