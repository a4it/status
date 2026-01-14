package org.automatize.status.models;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing the association between a maintenance window and an affected component.
 * <p>
 * StatusMaintenanceComponent is a join entity that links StatusMaintenance to StatusComponent,
 * allowing maintenance windows to specify which components will be affected during the
 * scheduled maintenance period.
 * </p>
 * <p>
 * This enables granular communication to users about which specific parts of a service
 * will be impacted during planned maintenance.
 * </p>
 *
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
