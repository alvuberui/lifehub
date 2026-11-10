package com.finanzapp.lifehub.modules.home.api;

import com.finanzapp.lifehub.modules.home.domain.AttachmentType;
import com.finanzapp.lifehub.modules.home.domain.HomeItemStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class HomeDtos {
    private HomeDtos() { }

    public record CategoryRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 32) String color,
            @Size(max = 64) String icon) { }

    public record CategoryResponse(Long id, String name, String color, String icon) { }

    public record ItemRequest(
            @NotBlank @Size(max = 180) String name,
            @NotBlank @Size(max = 2000) String description,
            @NotNull Long categoryId,
            @NotBlank @Size(max = 120) String brand,
            @NotBlank @Size(max = 120) String model,
            @Size(max = 160) String serialNumber,
            @Size(max = 160) String location,
            @NotBlank @Size(max = 180) String supplier,
            @NotNull HomeItemStatus status,
            @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 12, fraction = 2) BigDecimal price,
            @Size(min = 3, max = 3) String currency,
            @NotNull LocalDate purchaseDate,
            @NotNull LocalDate warrantyEndDate,
            Boolean shared) { }

    public record AttachmentResponse(Long id, String documentType, String originalFilename,
                                     String contentType, Long sizeBytes, Instant createdAt,
                                     String downloadUrl) { }

    public record ItemResponse(Long id, String ownerEmail, boolean shared, String name, String description, CategoryResponse category,
                               String brand, String model, String serialNumber, String location,
                               String supplier, HomeItemStatus status, BigDecimal price, String currency,
                               LocalDate purchaseDate, LocalDate warrantyEndDate, long daysUntilWarranty,
                               boolean archived, List<AttachmentResponse> attachments,
                               Instant createdAt, Instant updatedAt) { }
}
