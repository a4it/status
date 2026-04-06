package org.automatize.status.repositories;

import org.automatize.status.models.SchedulerRestConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link SchedulerRestConfig} entities.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Repository
public interface SchedulerRestConfigRepository extends JpaRepository<SchedulerRestConfig, UUID> {

    /**
     * Fetches the REST configuration for the given job ID.
     *
     * @param jobId the parent job UUID
     * @return an Optional containing the config if present
     */
    Optional<SchedulerRestConfig> findByJobId(UUID jobId);
}
