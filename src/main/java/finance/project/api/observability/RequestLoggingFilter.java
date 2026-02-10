package finance.project.api.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || (!uri.startsWith("/api") && !uri.startsWith("/actuator"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNs = System.nanoTime();
        String endpoint = request.getRequestURI();
        MDC.put("endpoint", endpoint);

        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            MDC.put("status", String.valueOf(response.getStatus()));
            MDC.put("latency_ms", String.valueOf(latencyMs));
            log.info("http_request");
            MDC.remove("latency_ms");
            MDC.remove("status");
            MDC.remove("specType");
            MDC.remove("requestId");
            MDC.remove("endpoint");
        }
    }
}

