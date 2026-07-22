package org.automatize.status.models;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.ZonedDateTime;

/**
 * <p>
 * JPA entity listener that maintains audit timestamps for {@link Auditable} entities.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Default creation and modification timestamps on persist if not already set</li>
 *   <li>Refresh modification timestamps on every update</li>
 *   <li>Keep technical epoch-millisecond timestamps in sync for sorting and querying</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see Auditable
 */
public class AuditTimestampListener {

    /**
     * JPA lifecycle callback executed before persisting a new entity.
     * Automatically sets creation and modification timestamps if not already set.
     *
     * @param entity the auditable entity being persisted
     */
    @PrePersist
    public void prePersist(Auditable entity) {
        ZonedDateTime now = ZonedDateTime.now();
        // Default the creation timestamp only if it has not been set
        if (entity.getCreatedDate() == null) {
            entity.setCreatedDate(now);
        }
        // Default the modification timestamp only if it has not been set
        if (entity.getLastModifiedDate() == null) {
            entity.setLastModifiedDate(now);
        }
        // Default the technical creation timestamp only if it has not been set
        if (entity.getCreatedDateTechnical() == null) {
            entity.setCreatedDateTechnical(System.currentTimeMillis());
        }
        // Default the technical modification timestamp only if it has not been set
        if (entity.getLastModifiedDateTechnical() == null) {
            entity.setLastModifiedDateTechnical(System.currentTimeMillis());
        }
    }

    /**
     * JPA lifecycle callback executed before updating an existing entity.
     * Automatically updates the modification timestamps.
     *
     * @param entity the auditable entity being updated
     */
    @PreUpdate
    public void preUpdate(Auditable entity) {
        entity.setLastModifiedDate(ZonedDateTime.now());
        entity.setLastModifiedDateTechnical(System.currentTimeMillis());
    }
}
