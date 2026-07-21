package org.automatize.status.api.response;

/**
 * <p>
 * Response object containing logger configuration details.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose a logger's name for the runtime log-level management UI</li>
 *   <li>Report the effective level actually in force for the logger</li>
 *   <li>Report the explicitly configured level, if any, distinct from the inherited one</li>
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
public class LoggerInfoResponse {

    /** The name of the logger. */
    private String name;

    /** The effective log level in force for the logger. */
    private String effectiveLevel;

    /** The explicitly configured log level, or null if inherited. */
    private String configuredLevel;

    /**
     * Default constructor.
     */
    public LoggerInfoResponse() {
    }

    /** Gets the logger name. @return the logger name */
    public String getName() { return name; }
    /** Sets the logger name. @param name the logger name to set */
    public void setName(String name) { this.name = name; }

    /** Gets the effective level. @return the effective level */
    public String getEffectiveLevel() { return effectiveLevel; }
    /** Sets the effective level. @param effectiveLevel the effective level to set */
    public void setEffectiveLevel(String effectiveLevel) { this.effectiveLevel = effectiveLevel; }

    /** Gets the configured level. @return the configured level, or null if inherited */
    public String getConfiguredLevel() { return configuredLevel; }
    /** Sets the configured level. @param configuredLevel the configured level to set */
    public void setConfiguredLevel(String configuredLevel) { this.configuredLevel = configuredLevel; }
}
