package org.automatize.status.api.response;

/**
 * <p>
 * Immutable response describing a single available help article as a listing
 * entry, without its full body, used to build the help/documentation index of
 * the status-monitoring application.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Identify the help article by its URL-friendly {@code slug}</li>
 *   <li>Expose the human-readable {@code title} for menus and links</li>
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
public record HelpFileResponse(String slug, String title) {}
