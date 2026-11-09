package com.graduation.procurement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "procurement")
public class PublicDeployProperties {
    private boolean publicMode;
    private String demoGateSecret = "";
    private String demoPassword = "";

    public boolean isPublicMode() { return publicMode; }
    public void setPublicMode(boolean publicMode) { this.publicMode = publicMode; }
    public String getDemoGateSecret() { return demoGateSecret == null ? "" : demoGateSecret; }
    public void setDemoGateSecret(String demoGateSecret) { this.demoGateSecret = demoGateSecret; }
    public String getDemoPassword() { return demoPassword == null ? "" : demoPassword; }
    public void setDemoPassword(String demoPassword) { this.demoPassword = demoPassword; }
}
