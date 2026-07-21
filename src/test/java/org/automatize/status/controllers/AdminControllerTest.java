package org.automatize.status.controllers;

import org.automatize.status.controllers.api.AbstractApiControllerTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@code @WebMvcTest} slice tests for {@link AdminController}. Every mapping
 * returns a static view name and sets the {@code activeNav} model attribute plus
 * common build attributes; these are asserted here.
 */
@WebMvcTest(controllers = AdminController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
class AdminControllerTest extends AbstractApiControllerTest {

    @Test
    void login_returnsAdminLoginView() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    @Test
    void dashboard_returnsDashboardViewWithCommonAttributes() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("activeNav", "dashboard"))
                .andExpect(model().attributeExists("applicationName", "buildNumber",
                        "buildDate", "copyright"));
    }

    /**
     * Covers every remaining {@code @GetMapping} that follows the identical
     * pattern: set {@code activeNav} to a fixed key and return a static view.
     *
     * @param path         request path under {@code /admin}
     * @param expectedNav  expected {@code activeNav} model attribute value
     * @param expectedView expected returned view name
     */
    @ParameterizedTest(name = "{0} -> {2}")
    @CsvSource({
            "/admin/platforms,platforms,admin/platforms",
            "/admin/issues,issues,admin/issues",
            "/admin/components,components,admin/components",
            "/admin/subscribers,subscribers,admin/subscribers",
            "/admin/events,events,admin/events",
            "/admin/health-checks,health-checks,admin/health-checks",
            "/admin/tenants,tenants,admin/tenants",
            "/admin/organizations,organizations,admin/organizations",
            "/admin/logs,logs,admin/logs",
            "/admin/drop-rules,drop-rules,admin/drop-rules",
            "/admin/log-metrics,log-metrics,admin/log-metrics",
            "/admin/alert-rules,alert-rules,admin/alert-rules",
            "/admin/log-api-keys,log-api-keys,admin/log-api-keys",
            "/admin/users,users,admin/users",
            "/admin/process-mining,process-mining,admin/process-mining",
            "/admin/process-timeline,process-timeline,admin/process-timeline",
            "/admin/select-context,select-context,admin/select-context",
            "/admin/help,help,admin/help",
            "/admin/jvm,jvm,admin/jvm",
            "/admin/log-viewer,log-viewer,admin/log-viewer",
            "/admin/data-retention,data-retention,admin/data-retention",
            "/admin/scheduler,scheduler,admin/scheduler/jobs",
            "/admin/scheduler/datasources,scheduler-datasources,admin/scheduler/datasources"
    })
    void adminPage_returnsExpectedViewAndActiveNav(String path, String expectedNav,
                                                   String expectedView) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name(expectedView))
                .andExpect(model().attribute("activeNav", expectedNav))
                .andExpect(model().attributeExists("applicationName", "buildNumber",
                        "buildDate", "copyright"));
    }
}
