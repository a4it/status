package org.automatize.status.api.request;

import java.util.Map;

/**
 * <p>
 * Request object carrying a bag of application configuration properties submitted
 * during the first-run setup wizard of the status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Transport arbitrary key/value configuration entries from the setup UI to the service layer</li>
 *   <li>Allow the setup flow to persist runtime application settings without a fixed schema</li>
 * </ul>
 * </p>
 *
 * @author Tim De Smedt
 */
public class SetupPropertiesRequest {

    /** The map of configuration property keys to values. */
    private Map<String, String> properties;

    /** @return the configuration property key/value map */
    public Map<String, String> getProperties() { return properties; }
    /** @param properties the configuration property key/value map to set */
    public void setProperties(Map<String, String> properties) { this.properties = properties; }
}
