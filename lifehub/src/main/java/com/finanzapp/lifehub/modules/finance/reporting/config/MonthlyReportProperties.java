package com.finanzapp.lifehub.modules.finance.reporting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lifehub.monthly-report")
public class MonthlyReportProperties {

    private boolean enabled;
    private boolean dryRun;
    private boolean testEndpointEnabled;
    private String timeZone = "Europe/Madrid";
    private String fromEmail;
    private String fromName = "LifeHub";
    private String brevoApiKey;
}
