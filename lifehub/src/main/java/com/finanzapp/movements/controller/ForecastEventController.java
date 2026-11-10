package com.finanzapp.movements.controller;

import com.finanzapp.movements.controller.dto.ForecastEventRequest;
import com.finanzapp.movements.controller.dto.ForecastEventResponse;
import com.finanzapp.movements.repository.ForecastEventRepository;
import com.finanzapp.movements.repository.entities.ForecastEventEntity;
import com.finanzapp.movements.repository.entities.ForecastEventType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/forecast-events")
@RequiredArgsConstructor
public class ForecastEventController {

    private final ForecastEventRepository forecastEventRepository;

    @GetMapping
    public ResponseEntity<List<ForecastEventResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(forecastEventRepository.findByUserEmailOrderByEventMonthAscIdAsc(email).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<ForecastEventResponse> create(
            @Valid @RequestBody ForecastEventRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = jwt.getClaimAsString("email");
        ForecastEventType type = ForecastEventType.valueOf(request.type().toUpperCase(Locale.ROOT));
        ForecastEventEntity entity = ForecastEventEntity.builder()
                .userEmail(email)
                .name(request.name().trim())
                .eventMonth(YearMonth.parse(request.month()).atDay(1))
                .amount(request.amount())
                .eventType(type)
                .createdAt(Instant.now())
                .build();
        return ResponseEntity.ok(toResponse(forecastEventRepository.save(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        forecastEventRepository.findByIdAndUserEmail(id, email).ifPresent(forecastEventRepository::delete);
        return ResponseEntity.noContent().build();
    }

    private ForecastEventResponse toResponse(ForecastEventEntity entity) {
        return new ForecastEventResponse(
                entity.getId(),
                entity.getName(),
                YearMonth.from(entity.getEventMonth()).toString(),
                entity.getAmount(),
                entity.getEventType().name().toLowerCase(Locale.ROOT)
        );
    }
}
