package org.automatize.status.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that intercepts all requests when the application has not yet been
 * set up (app.setup.completed=false) and redirects them to the /setup wizard page.
 *
 * Once setup completes, {@link #markSetupComplete()} is called by {@link org.automatize.status.services.SetupService}
 * to flip the in-memory flag so the redirect stops immediately without requiring a restart.
 *
 * The volatile flag is thread-safe for a single-write (false → true), never-reverted scenario.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SetupFilter extends OncePerRequestFilter {

    @Value("${app.setup.completed:false}")
    private boolean initialSetupCompleted;

    private volatile boolean setupCompleted;

    @PostConstruct
    public void init() {
        this.setupCompleted = initialSetupCompleted;
    }

    /**
     * Called by SetupService after writing app.setup.completed=true to disk.
     * Flips the in-memory flag so future requests pass through without restart.
     */
    public void markSetupComplete() {
        this.setupCompleted = true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (setupCompleted || isExcluded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/setup");
    }

    private boolean isExcluded(String path) {
        return path.startsWith("/setup")
                || path.startsWith("/api/setup")
                || path.startsWith("/static/")
                || path.startsWith("/error")
                || path.equals("/favicon.ico");
    }
}
