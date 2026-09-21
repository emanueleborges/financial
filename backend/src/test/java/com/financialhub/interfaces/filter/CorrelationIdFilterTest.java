package com.financialhub.interfaces.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldReuseSafeCorrelationIdAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-123");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals("request-123", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertNull(MDC.get("correlationId"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldGenerateCorrelationIdForUnsafeHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "bad value\n");

        filter.doFilter(request, response, mock(FilterChain.class));

        String correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertEquals(36, correlationId.length());
        assertFalse(correlationId.contains("\n"));
    }
}