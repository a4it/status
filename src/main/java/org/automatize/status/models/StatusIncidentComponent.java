package org.automatize.status.models;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing the association between an incident and an affected component.
 * <p>
 * StatusIncidentComponent is a join entity that links StatusIncident to StatusComponent,
 * allowing incidents to affect multiple components with potentially different status
 * impacts for each component.
 * </p>
 * <p>
 * This enables granular tracking of which specific components are affected by an incident
 * and what status each component should display during the incident.
 * </p>
 *
 * @see StatusIncident
 * @see StatusComponent
 */
@Entity
@Table(name = "status_incident_components")
public class StatusIncidentComponent {

    /**
     * Unique identifier for the incident-component association.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The incident that affects the component.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private StatusIncident incident;

    /**
     * The component affected by the incident.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    private StatusComponent component;

    /**
     * The status of the component during this incident.
     * Common values include OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE.
     */
    @Column(name = "component_status", nullable = false, length = 50)
    private String componentStatus;

    /**
     * Default constructor required by JPA.
     */
    public StatusIncidentComponent() {
    }

    /**
     * Gets the unique identifier of the incident-component association.
     *
     * @return the UUID of the association
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the incident-component association.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the incident that affects the component.
     *
     * @return the associated StatusIncident
     */
    public StatusIncident getIncident() {
        return incident;
    }

    /**
     * Sets the incident that affects the component.
     *
     * @param incident the StatusIncident to set
     */
    public void setIncident(StatusIncident incident) {
        this.incident = incident;
    }

    /**
     * Gets the component affected by the incident.
     *
     * @return the affected StatusComponent
     */
    public StatusComponent getComponent() {
        return component;
    }

    /**
     * Sets the component affected by the incident.
     *
     * @param component the StatusComponent to set
     */
    public void setComponent(StatusComponent component) {
        this.component = component;
    }

    /**
     * Gets the status of the component during this incident.
     *
     * @return the component status (e.g., DEGRADED, MAJOR_OUTAGE)
     */
    public String getComponentStatus() {
        return componentStatus;
    }

    /**
     * Sets the status of the component during this incident.
     *
     * @param componentStatus the component status to set
     */
    public void setComponentStatus(String componentStatus) {
        this.componentStatus = componentStatus;
    }
}
