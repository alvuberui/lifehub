package com.finanzapp.lifehub.modules.finance.reporting.service;

import com.finanzapp.lifehub.modules.finance.reporting.config.MonthlyReportProperties;
import com.finanzapp.lifehub.modules.finance.reporting.email.EmailSendResult;
import com.finanzapp.lifehub.modules.finance.reporting.email.MonthlyReportEmailSender;
import com.finanzapp.lifehub.modules.identity.reporting.repository.MonthlyReportDispatchRepository;
import com.finanzapp.lifehub.modules.identity.user.repository.UserRepository;
import com.finanzapp.lifehub.modules.identity.user.repository.entities.UserEntity;
import com.finanzapp.tags.repository.TagRepository;
import com.finanzapp.tags.repository.entities.TagEntity;
import com.finanzapp.movements.repository.MovementRepository;
import com.finanzapp.movements.repository.entities.MovementEntity;
import com.finanzapp.movements.repository.entities.MovementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", SPANISH);
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", SPANISH);

    private final UserRepository userRepository;
    private final MovementRepository movementRepository;
    private final TagRepository tagRepository;
    private final MonthlyReportDispatchRepository dispatchRepository;
    private final MonthlyReportEmailSender emailSender;
    private final MonthlyReportProperties properties;

    /**
     * Se ejecuta diariamente para que un reinicio el día 1 no haga perder el informe.
     * La tabla de dispatches garantiza que un usuario no reciba el mismo mes dos veces.
     */
    @Scheduled(
            cron = "${lifehub.monthly-report.cron:0 0 8 * * *}",
            zone = "${lifehub.monthly-report.time-zone:Europe/Madrid}"
    )
    public void sendPreviousMonthReports() {
        if (!properties.isEnabled()) {
            return;
        }
        if (!properties.isDryRun()
                && (isBlank(properties.getBrevoApiKey()) || isBlank(properties.getFromEmail()))) {
            log.warn("Resumen mensual activado pero falta BREVO_API_KEY o BREVO_FROM_EMAIL; no se enviarán emails");
            return;
        }

        YearMonth reportMonth = YearMonth.now(zoneId()).minusMonths(1);
        log.info("Procesando resúmenes mensuales de {}", reportMonth);

        for (UserEntity user : userRepository.findAll()) {
            if (isBlank(user.getEmail())) {
                continue;
            }
            try {
                if (properties.isDryRun()) {
                    MonthlyReport report = buildReport(user, reportMonth);
                    log.info("[DRY RUN] Resumen de {} para {}: ingresos={}, gastos={}, ahorro={}",
                            reportMonth, user.getEmail(), report.income(), report.totalExpenses(), report.savings());
                    continue;
                }
                processUserReport(user, reportMonth);
            } catch (Exception exception) {
                log.error("No se pudo procesar el resumen mensual de {} para {}", reportMonth, user.getEmail(), exception);
            }
        }
    }

    public String sendTestReport(String sourceUserEmail, String recipientEmail, YearMonth reportMonth) {
        if (!properties.isTestEndpointEnabled()) {
            throw new IllegalStateException("El endpoint de prueba está desactivado");
        }
        if (properties.isDryRun()) {
            throw new IllegalStateException("MONTHLY_REPORT_DRY_RUN debe estar en false para enviar una prueba");
        }

        UserEntity sourceUser = userRepository.findByEmail(sourceUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario origen no encontrado: " + sourceUserEmail));
        MonthlyReport report = buildReport(sourceUser, reportMonth);
        String subject = "Prueba · Tu resumen financiero de "
                + reportMonth.getMonth().getDisplayName(TextStyle.FULL, SPANISH) + " · LifeHub";
        EmailSendResult result = emailSender.send(
                recipientEmail,
                sourceUser.getName(),
                subject,
                renderHtml(report)
        );
        log.info("Resumen mensual de prueba enviado a {} usando los datos de {}", recipientEmail, sourceUserEmail);
        return result.providerMessageId();
    }

    private void processUserReport(UserEntity user, YearMonth reportMonth) {
        Instant now = Instant.now();
        int claimed = dispatchRepository.claimForProcessing(user.getEmail(), reportMonth.atDay(1), now);
        if (claimed == 0) {
            return;
        }

        try {
            MonthlyReport report = buildReport(user, reportMonth);
            String html = renderHtml(report);
            String subject = "Tu resumen financiero de " + reportMonth.getMonth().getDisplayName(TextStyle.FULL, SPANISH)
                    + " · LifeHub";
            EmailSendResult result = emailSender.send(
                    user.getEmail(),
                    user.getName(),
                    subject,
                    html
            );
            dispatchRepository.markSent(user.getEmail(), reportMonth.atDay(1), Instant.now(), result.providerMessageId());
            log.info("Resumen mensual enviado a {} para {}", user.getEmail(), reportMonth);
        } catch (Exception exception) {
            dispatchRepository.markFailed(
                    user.getEmail(),
                    reportMonth.atDay(1),
                    Instant.now(),
                    truncate(exception.getMessage(), 1900)
            );
            throw exception;
        }
    }

    private MonthlyReport buildReport(UserEntity user, YearMonth reportMonth) {
        List<MovementEntity> movements = loadMovements(user.getEmail(), reportMonth);
        List<MovementEntity> previousMovements = loadMovements(user.getEmail(), reportMonth.minusMonths(1));

        Map<Long, String> tagNames = loadTagNames(user.getEmail(), movements);
        return summarize(
                user,
                reportMonth,
                movements,
                previousMovements,
                tagNames
        );
    }

    private List<MovementEntity> loadMovements(String email, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.plusMonths(1).atDay(1).minusDays(1);
        return movementRepository.findByUserEmailAndDateBetweenOrderByDate(email, from, to);
    }

    private Map<Long, String> loadTagNames(String email, List<MovementEntity> movements) {
        Set<Long> tagIds = movements.stream()
                .map(MovementEntity::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (tagIds.isEmpty()) {
            return Map.of();
        }
        return tagRepository.findByUserEmailAndIdInAndDeletedFalse(email, new ArrayList<>(tagIds)).stream()
                .collect(Collectors.toMap(TagEntity::getId, TagEntity::getName));
    }

    private MonthlyReport summarize(
            UserEntity user,
            YearMonth reportMonth,
            List<MovementEntity> movements,
            List<MovementEntity> previousMovements,
            Map<Long, String> tagNames
    ) {
        BigDecimal income = sumByType(movements, MovementType.BENEFIT);
        BigDecimal necessaryExpenses = sumByType(movements, MovementType.ESSENTIAL_EXPENSE);
        BigDecimal discretionaryExpenses = sumByType(movements, MovementType.DISCRETIONARY_EXPENSE);
        BigDecimal investments = sumByType(movements, MovementType.INVESTMENT);
        BigDecimal totalExpenses = necessaryExpenses.add(discretionaryExpenses);
        BigDecimal savings = income.subtract(totalExpenses);
        BigDecimal balance = savings.subtract(investments);
        BigDecimal savingsRate = income.signum() == 0
                ? BigDecimal.ZERO
                : savings.multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP);

        BigDecimal previousIncome = sumByType(previousMovements, MovementType.BENEFIT);
        BigDecimal previousExpenses = sumByType(previousMovements, MovementType.ESSENTIAL_EXPENSE)
                .add(sumByType(previousMovements, MovementType.DISCRETIONARY_EXPENSE));
        BigDecimal previousBalance = previousIncome
                .subtract(previousExpenses)
                .subtract(sumByType(previousMovements, MovementType.INVESTMENT));

        Map<String, BigDecimal> expensesByTag = movements.stream()
                .filter(movement -> movement.getMovementType() == MovementType.ESSENTIAL_EXPENSE
                        || movement.getMovementType() == MovementType.DISCRETIONARY_EXPENSE)
                .collect(Collectors.groupingBy(
                        movement -> movement.getTagId() == null
                                ? "Sin etiqueta"
                                : tagNames.getOrDefault(movement.getTagId(), "Sin etiqueta"),
                        Collectors.reducing(BigDecimal.ZERO, this::amount, BigDecimal::add)
                ));

        List<CategoryTotal> categories = expensesByTag.entrySet().stream()
                .map(entry -> new CategoryTotal(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategoryTotal::amount).reversed())
                .limit(6)
                .toList();

        return new MonthlyReport(
                user.getName(),
                user.getEmail(),
                reportMonth,
                income,
                necessaryExpenses,
                discretionaryExpenses,
                investments,
                totalExpenses,
                savings,
                savingsRate,
                balance,
                previousBalance,
                percentChange(balance, previousBalance),
                movements.size(),
                categories
        );
    }

    private BigDecimal sumByType(List<MovementEntity> movements, MovementType type) {
        return movements.stream()
                .filter(movement -> movement.getMovementType() == type)
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal amount(MovementEntity movement) {
        return movement.getAmount() == null ? BigDecimal.ZERO : movement.getAmount();
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return current.signum() == 0 ? BigDecimal.ZERO : null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
    }

    private String renderHtml(MonthlyReport report) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(SPANISH);
        currency.setCurrency(java.util.Currency.getInstance("EUR"));
        String month = capitalize(report.reportMonth().format(MONTH_FORMATTER));
        String comparison = report.balanceChangePercent() == null
                ? "Sin datos del mes anterior"
                : signed(report.balanceChangePercent()) + "% frente al mes anterior";

        String categoryRows = report.categories().isEmpty()
                ? "<tr><td colspan=\"2\" style=\"padding:12px 0;color:#64748b\">Sin gastos etiquetados</td></tr>"
                : report.categories().stream()
                .map(category -> "<tr><td style=\"padding:8px 0;border-bottom:1px solid #e2e8f0\">"
                        + escape(category.name())
                        + "</td><td align=\"right\" style=\"padding:8px 0;border-bottom:1px solid #e2e8f0;font-weight:700\">"
                        + escape(currency.format(category.amount()))
                        + "</td></tr>")
                .collect(Collectors.joining());

        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    @media only screen and (max-width:620px) {
                      .metric-card { display:block !important; width:100%% !important; box-sizing:border-box !important; margin-bottom:8px !important; }
                      .metric-card-value { font-size:18px !important; }
                    }
                  </style>
                </head>
                <body style="margin:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#0f172a">
                  <div style="max-width:680px;margin:0 auto;padding:28px 16px">
                    <div style="background:linear-gradient(135deg,#0f172a,#164e63);border-radius:24px;padding:32px;color:white">
                      <div style="font-size:13px;letter-spacing:1.8px;text-transform:uppercase;color:#a5f3fc">LifeHub · Finanzas</div>
                      <h1 style="margin:12px 0 6px;font-size:30px">Tu resumen de %s</h1>
                      <p style="margin:0;color:#cbd5e1;font-size:15px">Hola %s, aquí tienes una visión clara de lo que ocurrió.</p>
                    </div>
                    <div style="background:white;border-radius:20px;padding:22px;margin-top:16px">
                      <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                        %s
                        %s
                        %s
                      </tr><tr>
                        %s
                        %s
                        %s
                      </tr></table>
                      <div style="margin-top:18px;padding:14px 16px;background:#ecfeff;border-radius:14px;color:#155e75;font-size:14px">
                        %s
                      </div>
                    </div>
                    <div style="background:white;border-radius:20px;padding:22px;margin-top:16px">
                      <h2 style="margin:0 0 12px;font-size:18px">Distribución de gastos</h2>
                      <table width="100%%" cellpadding="0" cellspacing="0">%s</table>
                    </div>
                    <div style="text-align:center;padding:22px 8px;color:#64748b;font-size:12px">
                      %d movimientos · Los movimientos extraordinarios se incluyen en este resumen real.<br>
                      Puedes consultar el detalle completo desde LifeHub.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escape(month),
                escape(displayName(report.recipientName(), report.recipientEmail())),
                card("Ingresos", currency.format(report.income()), "#ecfdf5", "#166534"),
                card("Gastos", currency.format(report.totalExpenses()), "#fff7ed", "#9a3412"),
                card("Ahorro real", currency.format(report.savings()), "#eff6ff", "#1d4ed8"),
                card("Necesarios", currency.format(report.necessaryExpenses()), "#f8fafc", "#334155"),
                card("Discrecionales", currency.format(report.discretionaryExpenses()), "#f8fafc", "#334155"),
                card("Inversiones", currency.format(report.investments()), "#f8fafc", "#334155"),
                        escape("Balance: " + currency.format(report.balance()) + ". "
                                + comparison + ". Tasa de ahorro: " + report.savingsRate() + "%"),
                categoryRows,
                report.movementCount()
        );
    }

    private String card(String title, String value, String background, String color) {
        return "<td class=\"metric-card\" width=\"33%%\" style=\"padding:8px;background:%s;border-radius:14px;color:%s;vertical-align:top\"><div style=\"font-size:12px\">%s</div><div class=\"metric-card-value\" style=\"font-size:20px;font-weight:700;margin-top:6px\">%s</div></td>"
                .formatted(background, color, escape(title), escape(value));
    }

    private String displayName(String name, String email) {
        return isBlank(name) ? email : name;
    }

    private String signed(BigDecimal value) {
        return value.signum() > 0 ? "+" + value : value.toString();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        return value.substring(0, 1).toUpperCase(SPANISH) + value.substring(1);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) return "Error desconocido";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ZoneId zoneId() {
        try {
            return ZoneId.of(properties.getTimeZone());
        } catch (Exception exception) {
            log.warn("Zona horaria inválida '{}'; se usará Europe/Madrid", properties.getTimeZone());
            return ZoneId.of("Europe/Madrid");
        }
    }

    private record CategoryTotal(String name, BigDecimal amount) {
    }

    private record MonthlyReport(
            String recipientName,
            String recipientEmail,
            YearMonth reportMonth,
            BigDecimal income,
            BigDecimal necessaryExpenses,
            BigDecimal discretionaryExpenses,
            BigDecimal investments,
            BigDecimal totalExpenses,
            BigDecimal savings,
            BigDecimal savingsRate,
            BigDecimal balance,
            BigDecimal previousBalance,
            BigDecimal balanceChangePercent,
            int movementCount,
            List<CategoryTotal> categories
    ) {
    }
}
