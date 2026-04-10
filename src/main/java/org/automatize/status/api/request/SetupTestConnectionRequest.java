package org.automatize.status.api.request;

import jakarta.validation.constraints.NotBlank;

public class SetupTestConnectionRequest {

    @NotBlank
    private String url;

    @NotBlank
    private String username;

    private String password;

    private boolean saveToProperties;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isSaveToProperties() { return saveToProperties; }
    public void setSaveToProperties(boolean saveToProperties) { this.saveToProperties = saveToProperties; }
}
