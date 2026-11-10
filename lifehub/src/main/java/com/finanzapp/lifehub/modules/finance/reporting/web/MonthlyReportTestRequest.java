package com.finanzapp.lifehub.modules.finance.reporting.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MonthlyReportTestRequest(
        @NotBlank @Email String sourceUserEmail,
        @NotBlank @Email String recipientEmail
) {
}
