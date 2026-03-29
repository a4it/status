package org.automatize.status.api.response;

public class LoggerInfoResponse {

    private String name;
    private String effectiveLevel;
    private String configuredLevel;

    public LoggerInfoResponse() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEffectiveLevel() { return effectiveLevel; }
    public void setEffectiveLevel(String effectiveLevel) { this.effectiveLevel = effectiveLevel; }

    public String getConfiguredLevel() { return configuredLevel; }
    public void setConfiguredLevel(String configuredLevel) { this.configuredLevel = configuredLevel; }
}
