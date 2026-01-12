package org.automatize.status.models;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "status_incident_components")
public class StatusIncidentComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private StatusIncident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    private StatusComponent component;

    @Column(name = "component_status", nullable = false, length = 50)
    private String componentStatus;

    public StatusIncidentComponent() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatusIncident getIncident() {
        return incident;
    }

    public void setIncident(StatusIncident incident) {
        this.incident = incident;
    }

    public StatusComponent getComponent() {
        return component;
    }

    public void setComponent(StatusComponent component) {
        this.component = component;
    }

    public String getComponentStatus() {
        return componentStatus;
    }

    public void setComponentStatus(String componentStatus) {
        this.componentStatus = componentStatus;
    }
}