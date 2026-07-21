package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * Request object for validating an external connection (such as a datasource or
 * service endpoint) during the first-run setup wizard of the status monitoring system.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Carry the connection URL and credentials to be tested by the service layer</li>
 *   <li>Validate that the URL and username are present</li>
 *   <li>Indicate whether a successful connection's details should be persisted to the application properties</li>
 * </ul>
 * </p>
 *
 * @author Tim De Smedt
 */
public class SetupTestConnectionRequest {

    /** The connection URL to test. */
    @NotBlank
    private String url;

    /** The username used for the connection. */
    @NotBlank
    private String username;

    /** The password used for the connection. */
    private String password;

    /** Whether the connection details should be saved to the application properties on success. */
    private boolean saveToProperties;

    /** @return the connection URL to test */
    public String getUrl() { return url; }
    /** @param url the connection URL to test */
    public void setUrl(String url) { this.url = url; }

    /** @return the connection username */
    public String getUsername() { return username; }
    /** @param username the connection username to set */
    public void setUsername(String username) { this.username = username; }

    /** @return the connection password */
    public String getPassword() { return password; }
    /** @param password the connection password to set */
    public void setPassword(String password) { this.password = password; }

    /** @return whether the connection details should be saved to the application properties */
    public boolean isSaveToProperties() { return saveToProperties; }
    /** @param saveToProperties whether the connection details should be saved to the application properties */
    public void setSaveToProperties(boolean saveToProperties) { this.saveToProperties = saveToProperties; }
}
