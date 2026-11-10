package com.finanzapp.lifehub.modules.finance.reporting.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finanzapp.lifehub.modules.finance.reporting.config.MonthlyReportProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
@RequiredArgsConstructor
public class BrevoMonthlyReportEmailSender implements MonthlyReportEmailSender {

    private static final URI BREVO_SEND_EMAIL_URI = URI.create("https://api.brevo.com/v3/smtp/email");

    private final ObjectMapper objectMapper;
    private final MonthlyReportProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public EmailSendResult send(String recipientEmail, String recipientName, String subject, String htmlContent) {
        if (properties.getBrevoApiKey() == null || properties.getBrevoApiKey().isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY no está configurada");
        }
        if (properties.getFromEmail() == null || properties.getFromEmail().isBlank()) {
            throw new IllegalStateException("BREVO_FROM_EMAIL no está configurada");
        }

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode sender = payload.putObject("sender");
            sender.put("name", properties.getFromName());
            sender.put("email", properties.getFromEmail());

            ArrayNode recipients = payload.putArray("to");
            ObjectNode recipient = recipients.addObject();
            recipient.put("email", recipientEmail);
            if (recipientName != null && !recipientName.isBlank()) {
                recipient.put("name", recipientName);
            }
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            HttpRequest request = HttpRequest.newBuilder(BREVO_SEND_EMAIL_URI)
                    .header("accept", "application/json")
                    .header("api-key", properties.getBrevoApiKey())
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Brevo devolvió HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode responseBody = response.body() == null || response.body().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.body());
            return new EmailSendResult(responseBody.path("messageId").asText(null));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("El envío del email fue interrumpido", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo conectar con Brevo", exception);
        }
    }
}
