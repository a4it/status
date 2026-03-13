package org.automatize.status.models;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * <p>
 * Entity representing the association between a maintenance window and an affected component.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Link maintenance windows to affected components as a join entity</li>
 *   <li>Specify which components will be affected during maintenance</li>
 *   <li>Enable granular communication about maintenance impact</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusMaintenance
 * @see StatusComponent
 */
@Entity
@Table(name = "status_maintenance_components")
public class StatusMaintenanceComponent {

    /**
     * Unique identifier for the maintenance-component association.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The maintenance window that affects the component.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_id", nullable = false)
    private StatusMaintenance maintenance;

    /**
     * The component affected by the maintenance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    private StatusComponent component;

    /**
     * Default constructor required by JPA.
     */
    public StatusMaintenanceComponent() {
    }

    /**
     * Gets the unique identifier of the maintenance-component association.
     *
     * @return the UUID of the association
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the maintenance-component association.
     *
     * @param id the UUID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the maintenance window that affects the component.
     *
     * @return the associated StatusMaintenance
     */
    public StatusMaintenance getMaintenance() {
        return maintenance;
    }

    /**
     * Sets the maintenance window that affects the component.
     *
     * @param maintenance the StatusMaintenance to set
     */
    public void setMaintenance(StatusMaintenance maintenance) {
        this.maintenance = maintenance;
    }

    /**
     * Gets the component affected by the maintenance.
     *
     * @return the affected StatusComponent
     */
    public StatusComponent getComponent() {
        return component;
    }

    /**
     * Sets the component affected by the maintenance.
     *
     * @param component the StatusComponent to set
     */
    public void setComponent(StatusComponent component) {
        this.component = component;
    }
}
