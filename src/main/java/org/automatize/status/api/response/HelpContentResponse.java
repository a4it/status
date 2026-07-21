package org.automatize.status.api.response;

/**
 * <p>
 * Immutable response carrying the fully rendered content of a single help
 * article for the status-monitoring application's in-app help/documentation
 * system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Identify the help article by its URL-friendly {@code slug}</li>
 *   <li>Expose the human-readable {@code title} for display</li>
 *   <li>Carry the pre-rendered {@code html} body of the article</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public record HelpContentResponse(String slug, String title, String html) {}
