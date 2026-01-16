package org.automatize.status.repositories;

import org.automatize.status.models.NotificationSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link NotificationSubscriber} entities.
 * <p>
 * Provides data access operations for notification subscribers within the system.
 * Subscribers are associated with status applications and receive email notifications
 * when incidents occur.
 * </p>
 *
 * @see NotificationSubscriber
 * @see org.automatize.status.models.StatusApp
 */
@Repository
public interface NotificationSubscriberRepository extends JpaRepository<NotificationSubscriber, UUID> {

    /**
     * Finds all subscribers for a specific status application.
     *
     * @param appId the unique identifier of the status application
     * @return a list of subscribers for the specified application
     */
    List<NotificationSubscriber> findByAppId(UUID appId);

    /**
     * Finds all active subscribers for a specific status application.
     *
     * @param appId the unique identifier of the status application
     * @param isActive the active status filter
     * @return a list of active subscribers for the specified application
     */
    List<NotificationSubscriber> findByAppIdAndIsActive(UUID appId, Boolean isActive);

    /**
     * Finds all active and verified subscribers for a specific status application.
     * These are the subscribers who should receive incident notifications.
     *
     * @param appId the unique identifier of the status application
     * @return a list of active and verified subscribers
     */
    @Query("SELECT s FROM NotificationSubscriber s WHERE s.app.id = :appId AND s.isActive = true AND s.isVerified = true")
    List<NotificationSubscriber> findActiveVerifiedByAppId(@Param("appId") UUID appId);

    /**
     * Finds a subscriber by email address within a specific application.
     *
     * @param appId the unique identifier of the status application
     * @param email the email address to search for
     * @return an Optional containing the subscriber if found
     */
    Optional<NotificationSubscriber> findByAppIdAndEmail(UUID appId, String email);

    /**
     * Finds a subscriber by their verification token.
     *
     * @param verificationToken the verification token
     * @return an Optional containing the subscriber if found
     */
    Optional<NotificationSubscriber> findByVerificationToken(String verificationToken);

    /**
     * Checks if a subscriber with the specified email exists for an application.
     *
     * @param appId the unique identifier of the status application
     * @param email the email address to check
     * @return true if a subscriber with the email exists, false otherwise
     */
    boolean existsByAppIdAndEmail(UUID appId, String email);

    /**
     * Counts the total number of subscribers for a specific application.
     *
     * @param appId the unique identifier of the status application
     * @return the count of subscribers
     */
    @Query("SELECT COUNT(s) FROM NotificationSubscriber s WHERE s.app.id = :appId")
    Long countByAppId(@Param("appId") UUID appId);

    /**
     * Counts the number of active subscribers for a specific application.
     *
     * @param appId the unique identifier of the status application
     * @return the count of active subscribers
     */
    @Query("SELECT COUNT(s) FROM NotificationSubscriber s WHERE s.app.id = :appId AND s.isActive = true")
    Long countActiveByAppId(@Param("appId") UUID appId);

    /**
     * Searches for subscribers by email or name within an application.
     *
     * @param appId the unique identifier of the status application
     * @param searchTerm the term to search for
     * @return a list of subscribers matching the search criteria
     */
    @Query("SELECT s FROM NotificationSubscriber s WHERE s.app.id = :appId AND (s.email LIKE %:searchTerm% OR s.name LIKE %:searchTerm%)")
    List<NotificationSubscriber> searchByAppId(@Param("appId") UUID appId, @Param("searchTerm") String searchTerm);

    /**
     * Deletes all subscribers for a specific application.
     *
     * @param appId the unique identifier of the status application
     */
    void deleteByAppId(UUID appId);
}
