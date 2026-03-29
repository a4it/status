package org.automatize.status.api.request;

public class GcScheduleRequest {

    private boolean enabled;
    private String cron;

    public GcScheduleRequest() {
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
}
