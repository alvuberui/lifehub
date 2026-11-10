package com.finanzapp.lifehub.modules.finance.reporting.email;

public interface MonthlyReportEmailSender {

    EmailSendResult send(String recipientEmail, String recipientName, String subject, String htmlContent);
}
