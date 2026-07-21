package org.automatize.status.api.response;

/**
 * <p>
 * Immutable response record representing a single hit from a help documentation search.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Identify the matched help article via its slug and title</li>
 *   <li>Provide a short excerpt highlighting the matched content</li>
 *   <li>Convey how many times the search term matched within the article</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @param slug       the URL-friendly identifier of the matched help article
 * @param title      the title of the matched help article
 * @param excerpt    a short excerpt showing the matched content
 * @param matchCount the number of times the search term matched within the article
 * @author Tim De Smedt
 */
public record HelpSearchResult(String slug, String title, String excerpt, int matchCount) {}
