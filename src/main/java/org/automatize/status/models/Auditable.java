package org.automatize.status.models;

import java.time.ZonedDateTime;

/**
 * <p>
 * Contract for entities that carry audit timestamp fields.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose creation and modification timestamps for automatic auditing</li>
 *   <li>Expose technical epoch-millisecond timestamps for efficient sorting and querying</li>
 *   <li>Enable shared lifecycle handling via {@link AuditTimestampListener}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see AuditTimestampListener
 */
public interface Auditable {

    /**
     * Gets the creation timestamp of the entity.
     *
     * @return the creation date and time
     */
    ZonedDateTime getCreatedDate();

    /**
     * Sets the creation timestamp of the entity.
     *
     * @param createdDate the creation date and time to set
     */
    void setCreatedDate(ZonedDateTime createdDate);

    /**
     * Gets the last modification timestamp of the entity.
     *
     * @return the last modification date and time
     */
    ZonedDateTime getLastModifiedDate();

    /**
     * Sets the last modification timestamp of the entity.
     *
     * @param lastModifiedDate the last modification date and time to set
     */
    void setLastModifiedDate(ZonedDateTime lastModifiedDate);

    /**
     * Gets the technical creation timestamp in epoch milliseconds.
     *
     * @return the creation timestamp in milliseconds since epoch
     */
    Long getCreatedDateTechnical();

    /**
     * Sets the technical creation timestamp in epoch milliseconds.
     *
     * @param createdDateTechnical the creation timestamp in milliseconds to set
     */
    void setCreatedDateTechnical(Long createdDateTechnical);

    /**
     * Gets the technical last modification timestamp in epoch milliseconds.
     *
     * @return the last modification timestamp in milliseconds since epoch
     */
    Long getLastModifiedDateTechnical();

    /**
     * Sets the technical last modification timestamp in epoch milliseconds.
     *
     * @param lastModifiedDateTechnical the last modification timestamp in milliseconds to set
     */
    void setLastModifiedDateTechnical(Long lastModifiedDateTechnical);
}
