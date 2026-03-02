package com.platform.partner.filter;

import com.platform.shared.util.CorrelationIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String correlationId = CorrelationIdUtil.getOrGenerate(
                    request.getHeader(CORRELATION_ID_HEADER)
            );

            // Put into MDC so every log line includes it automatically
            MDC.put("correlationId", correlationId);
            MDC.put("method", request.getMethod());
            MDC.put("path", request.getRequestURI());

            // Echo back in response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL: always clear MDC to prevent thread pool leaks
            MDC.clear();
        }
    }
}