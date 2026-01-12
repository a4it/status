package org.automatize.status.models;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "status_maintenance_components")
public class StatusMaintenanceComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_id", nullable = false)
    private StatusMaintenance maintenance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    private StatusComponent component;

    public StatusMaintenanceComponent() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatusMaintenance getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(StatusMaintenance maintenance) {
        this.maintenance = maintenance;
    }

    public StatusComponent getComponent() {
        return component;
    }

    public void setComponent(StatusComponent component) {
        this.component = component;
    }
}