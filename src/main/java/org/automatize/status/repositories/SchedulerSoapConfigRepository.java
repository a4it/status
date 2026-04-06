package org.automatize.status.repositories;

import org.automatize.status.models.SchedulerSoapConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link SchedulerSoapConfig} entities.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@Repository
public interface SchedulerSoapConfigRepository extends JpaRepository<SchedulerSoapConfig, UUID> {

    /**
     * Fetches the SOAP configuration for the given job ID.
     *
     * @param jobId the parent job UUID
     * @return an Optional containing the config if present
     */
    Optional<SchedulerSoapConfig> findByJobId(UUID jobId);
}
