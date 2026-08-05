package org.automatize.status.services;

import org.automatize.status.api.response.HealthCheckStatusResponse;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusPlatform;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.util.OffsetPageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HealthCheckStatusService}, which streams apps and components as a single
 * paginated result while leaving all filtering to the database.
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckStatusServiceTest {

    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private StatusComponentRepository statusComponentRepository;

    @InjectMocks
    private HealthCheckStatusService service;

    /**
     * Builds an app fixture carrying the given name and a platform association.
     *
     * @param name the app name
     * @return a populated {@link StatusApp}
     */
    private StatusApp app(String name) {
        StatusPlatform platform = new StatusPlatform();
        platform.setId(UUID.randomUUID());
        platform.setName("Platform");

        StatusApp app = new StatusApp();
        app.setId(UUID.randomUUID());
        app.setName(name);
        app.setPlatform(platform);
        app.setStatus("OPERATIONAL");
        return app;
    }

    /**
     * Builds a component fixture belonging to the given app.
     *
     * @param name the component name
     * @param parent the parent app
     * @return a populated {@link StatusComponent}
     */
    private StatusComponent component(String name, StatusApp parent) {
        StatusComponent component = new StatusComponent();
        component.setId(UUID.randomUUID());
        component.setName(name);
        component.setApp(parent);
        component.setStatus("OPERATIONAL");
        return component;
    }

    /**
     * Verifies apps and components are combined into one result whose total spans both sources,
     * with the app rows first and each row tagged with its entity type.
     */
    @Test
    void combinesAppsAndComponentsIntoOnePage() {
        StatusApp app = app("App");
        when(statusAppRepository.countForHealthCheckStatus(null, null, null)).thenReturn(1L);
        when(statusComponentRepository.countForHealthCheckStatus(null, null, null)).thenReturn(1L);
        when(statusAppRepository.findForHealthCheckStatus(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(statusComponentRepository.findForHealthCheckStatus(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(component("Comp", app))));

        Page<HealthCheckStatusResponse> page =
                service.getHealthCheckStatus(null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(HealthCheckStatusResponse::getEntityType)
                .containsExactly("APP", "COMPONENT");
        assertThat(page.getContent()).extracting(HealthCheckStatusResponse::getName)
                .containsExactly("App", "Comp");
        assertThat(page.getContent().get(0).getPlatformName()).isEqualTo("Platform");
        assertThat(page.getContent().get(1).getPlatformName()).isEqualTo("Platform");
    }

    /**
     * Verifies a page that straddles the app/component boundary reads only the remaining rows
     * from the components query, offset by the number of apps already consumed.
     */
    @Test
    void pageStraddlingTheBoundaryOffsetsTheComponentQuery() {
        StatusApp app = app("App3");
        when(statusAppRepository.countForHealthCheckStatus(null, null, null)).thenReturn(3L);
        when(statusComponentRepository.countForHealthCheckStatus(null, null, null)).thenReturn(5L);
        when(statusAppRepository.findForHealthCheckStatus(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(statusComponentRepository.findForHealthCheckStatus(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(component("C1", app))));

        // Rows 2..3 of 8: the last app plus the first component
        Page<HealthCheckStatusResponse> page =
                service.getHealthCheckStatus(null, null, null, PageRequest.of(1, 2));

        ArgumentCaptor<Pageable> appWindow = ArgumentCaptor.forClass(Pageable.class);
        verify(statusAppRepository).findForHealthCheckStatus(eq(null), eq(null), eq(null), appWindow.capture());
        assertThat(appWindow.getValue().getOffset()).isEqualTo(2);

        ArgumentCaptor<Pageable> componentWindow = ArgumentCaptor.forClass(Pageable.class);
        verify(statusComponentRepository)
                .findForHealthCheckStatus(eq(null), eq(null), eq(null), componentWindow.capture());
        // Three apps precede the components, so the component window starts at its own row 0
        assertThat(componentWindow.getValue().getOffset()).isZero();
        assertThat(componentWindow.getValue().getPageSize()).isEqualTo(1);

        assertThat(page.getTotalElements()).isEqualTo(8);
        assertThat(page.getContent()).extracting(HealthCheckStatusResponse::getEntityType)
                .containsExactly("APP", "COMPONENT");
    }

    /**
     * Verifies a page positioned entirely past the apps skips the app query and reads the
     * components from the correct offset.
     */
    @Test
    void pagePastTheAppsSkipsTheAppQuery() {
        StatusApp app = app("App");
        when(statusAppRepository.countForHealthCheckStatus(null, null, null)).thenReturn(2L);
        when(statusComponentRepository.countForHealthCheckStatus(null, null, null)).thenReturn(6L);
        when(statusComponentRepository.findForHealthCheckStatus(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(component("C3", app), component("C4", app))));

        Page<HealthCheckStatusResponse> page =
                service.getHealthCheckStatus(null, null, null, PageRequest.of(2, 2));

        verify(statusAppRepository, never())
                .findForHealthCheckStatus(any(), any(), any(), any(Pageable.class));

        ArgumentCaptor<Pageable> componentWindow = ArgumentCaptor.forClass(Pageable.class);
        verify(statusComponentRepository)
                .findForHealthCheckStatus(eq(null), eq(null), eq(null), componentWindow.capture());
        // Rows 4..5 overall, minus the two apps, is component row 2
        assertThat(componentWindow.getValue().getOffset()).isEqualTo(2);

        assertThat(page.getContent()).extracting(HealthCheckStatusResponse::getEntityType)
                .containsOnly("COMPONENT");
    }

    /**
     * Verifies the supplied filters are passed straight through to both repositories rather than
     * being applied to the loaded entities.
     */
    @Test
    void passesFiltersThroughToBothRepositories() {
        UUID platformId = UUID.randomUUID();
        when(statusAppRepository.countForHealthCheckStatus(platformId, "DEGRADED", true)).thenReturn(0L);
        when(statusComponentRepository.countForHealthCheckStatus(platformId, "DEGRADED", true)).thenReturn(0L);
        when(statusComponentRepository.findForHealthCheckStatus(
                eq(platformId), eq("DEGRADED"), eq(true), any(OffsetPageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<HealthCheckStatusResponse> page =
                service.getHealthCheckStatus(platformId, "DEGRADED", true, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }
}
