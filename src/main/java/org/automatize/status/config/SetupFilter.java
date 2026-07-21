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

    /**
     * Initialises the in-memory setup flag from the configured property value
     * after the bean has been constructed.
     */
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

    /**
     * Redirects every request to the setup wizard while the application has not
     * yet been set up, unless the request targets an excluded path.
     *
     * @param request     the incoming HTTP request
     * @param response     the HTTP response used to issue the redirect
     * @param filterChain the filter chain to continue processing when allowed
     * @throws ServletException if the downstream filter chain raises a servlet error
     * @throws IOException      if writing the redirect or continuing the chain fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Setup already done, or the path is exempt — let the request proceed normally
        if (setupCompleted || isExcluded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/setup");
    }

    /**
     * Determines whether the given request path should bypass the setup redirect
     * (the setup wizard itself, its API, static resources, error pages and the favicon).
     *
     * @param path the request URI to evaluate
     * @return {@code true} if the path is excluded from the setup redirect, {@code false} otherwise
     */
    private boolean isExcluded(String path) {
        return path.startsWith("/setup")
                || path.startsWith("/api/setup")
                || path.startsWith("/static/")
                || path.startsWith("/error")
                || path.equals("/favicon.ico");
    }
}
