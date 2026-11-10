package com.finanzapp.lifehub.modules.finance.reporting.web;

import com.finanzapp.lifehub.modules.finance.reporting.config.MonthlyReportProperties;
import com.finanzapp.lifehub.modules.finance.reporting.service.MonthlyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/internal/monthly-report")
@RequiredArgsConstructor
public class MonthlyReportTestController {

    private final MonthlyReportService monthlyReportService;
    private final MonthlyReportProperties properties;

    @Value("${services.internal-service-token}")
    private String internalServiceToken;

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestReport(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String providedToken,
            @Valid @RequestBody MonthlyReportTestRequest request
    ) {
        if (!properties.isTestEndpointEnabled()) {
            return ResponseEntity.notFound().build();
        }
        if (providedToken == null || !providedToken.equals(internalServiceToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Token interno no válido"));
        }

        String providerMessageId = monthlyReportService.sendTestReport(
                request.sourceUserEmail(),
                request.recipientEmail(),
                YearMonth.now(java.time.ZoneId.of(properties.getTimeZone())).minusMonths(1)
        );
        return ResponseEntity.ok(Map.of(
                "sent", true,
                "providerMessageId", providerMessageId == null ? "" : providerMessageId
        ));
    }
}
