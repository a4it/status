package org.automatize.status.api.request;

import java.util.Map;

public class SetupPropertiesRequest {

    private Map<String, String> properties;

    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) { this.properties = properties; }
}
