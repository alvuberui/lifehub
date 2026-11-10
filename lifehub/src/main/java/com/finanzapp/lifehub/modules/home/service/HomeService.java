package com.finanzapp.lifehub.modules.home.service;

import com.finanzapp.lifehub.modules.home.api.HomeDtos.*;
import com.finanzapp.lifehub.modules.home.domain.AttachmentType;
import com.finanzapp.lifehub.modules.home.domain.HomeItemStatus;
import com.finanzapp.lifehub.modules.home.repository.*;
import com.finanzapp.lifehub.modules.connections.domain.ModulePermission;
import com.finanzapp.lifehub.modules.connections.service.ConnectionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final HomeCategoryRepository categories;
    private final HomeItemRepository items;
    private final HomeAttachmentRepository attachments;
    private final HomeStorageService storage;
    private final ConnectionsService connections;

    public List<CategoryResponse> categories(String owner) {
        return categories(owner, "personal");
    }

    public List<CategoryResponse> categories(String owner, String scope) {
        List<String> owners = new ArrayList<>();
        owners.add(owner);
        if (scopeIsShared(scope)) {
            accessibleOwners(owner).stream()
                    .filter(candidate -> !candidate.equalsIgnoreCase(owner))
                    .forEach(owners::add);
        }

        Map<String, CategoryResponse> uniqueByName = new LinkedHashMap<>();
        for (String categoryOwner : owners) {
            categories.findAllByOwnerEmailInOrderByNameAsc(Set.of(categoryOwner)).stream()
                    .map(this::category)
                    .forEach(response -> uniqueByName.putIfAbsent(categoryKey(response.name()), response));
        }
        return uniqueByName.values().stream()
                .sorted(Comparator.comparing(CategoryResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public CategoryResponse createCategory(String owner, CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByOwnerEmailAndNameIgnoreCase(owner, name)) throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        Instant now = Instant.now();
        return category(categories.save(HomeCategoryEntity.builder().ownerEmail(owner).name(name)
                .color(blankToNull(request.color())).icon(blankToNull(request.icon())).createdAt(now).updatedAt(now).build()));
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public void deleteCategory(String owner, Long id) {
        HomeCategoryEntity category = categories.findById(id).orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        requireAdminOrOwner(owner, category.getOwnerEmail());
        if (items.existsByCategoryId(category.getId())) {
            throw new IllegalArgumentException("No puedes eliminar una categoría que tiene artículos asociados");
        }
        categories.delete(category);
    }

    public List<ItemResponse> search(String owner, String query, Long categoryId, HomeItemStatus status) {
        return search(owner, "personal", query, categoryId, status);
    }

    public List<ItemResponse> search(String owner, String scope, String query, Long categoryId, HomeItemStatus status) {
        boolean shared = scopeIsShared(scope);
        Set<String> owners = shared ? accessibleOwners(owner) : Set.of(owner);
        return items.searchByOwnersAndShared(owners, shared, query == null ? "" : query.trim(), categoryId, status)
                .stream().map(this::toResponse).toList();
    }

    public List<ItemResponse> archived(String owner) {
        return archived(owner, "personal");
    }

    public List<ItemResponse> archived(String owner, String scope) {
        boolean shared = scopeIsShared(scope);
        Set<String> owners = shared ? accessibleOwners(owner) : Set.of(owner);
        return items.findArchivedByOwnersAndShared(owners, shared).stream().map(this::toResponse).toList();
    }

    public List<ItemResponse> expiring(String owner, int days) {
        return expiring(owner, "personal", days);
    }

    public List<ItemResponse> expiring(String owner, String scope, int days) {
        if (days < 1 || days > 365) throw new IllegalArgumentException("El rango debe estar entre 1 y 365 días");
        LocalDate today = LocalDate.now();
        boolean shared = scopeIsShared(scope);
        Set<String> owners = shared ? accessibleOwners(owner) : Set.of(owner);
        List<HomeItemEntity> result = shared
                ? items.findSharedWarrantyExpiring(owners, today, today.plusDays(days))
                : items.searchByOwnersAndShared(owners, false, "", null, null).stream()
                .filter(item -> !item.getWarrantyEndDate().isBefore(today) && !item.getWarrantyEndDate().isAfter(today.plusDays(days)))
                .toList();
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public ItemResponse createItem(String owner, ItemRequest request, List<MultipartFile> files, List<AttachmentType> fileTypes) {
        validateDates(request);
        if (files == null || files.stream().filter(f -> f != null && !f.isEmpty()).count() == 0) {
            throw new IllegalArgumentException("Debes adjuntar al menos una foto del ticket o garantía");
        }
        HomeCategoryEntity category = categories.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        requireCategoryAccess(owner, category.getOwnerEmail());
        Instant now = Instant.now();
        HomeItemEntity item = items.save(HomeItemEntity.builder().ownerEmail(owner).shared(Boolean.TRUE.equals(request.shared())).name(request.name().trim())
                .description(request.description().trim()).categoryId(category.getId()).brand(request.brand().trim())
                .model(request.model().trim()).serialNumber(blankToNull(request.serialNumber())).location(blankToNull(request.location()))
                .supplier(request.supplier().trim()).status(request.status()).price(request.price())
                .currency(normalizeCurrency(request.currency())).purchaseDate(request.purchaseDate())
                .warrantyEndDate(request.warrantyEndDate()).createdAt(now).updatedAt(now).build());
        int fileIndex = 0;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                AttachmentType type = fileTypes != null && fileIndex < fileTypes.size() ? fileTypes.get(fileIndex) : AttachmentType.OTHER;
                saveAttachment(owner, item.getId(), file, type);
                fileIndex++;
            }
        }
        return toResponse(item);
    }

    public ItemResponse getItem(String owner, Long id) {
        return toResponse(requireAccessibleItem(owner, id));
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public ItemResponse updateItem(String owner, Long id, ItemRequest request) {
        validateDates(request);
        HomeItemEntity item = requireAccessibleItem(owner, id);
        if (item.getArchivedAt() != null) throw new IllegalArgumentException("No puedes editar un artículo archivado");
        HomeCategoryEntity category = categories.findById(request.categoryId()).orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        requireCategoryAccess(owner, category.getOwnerEmail());
        item.setName(request.name().trim()); item.setDescription(request.description().trim()); item.setCategoryId(request.categoryId());
        item.setBrand(request.brand().trim()); item.setModel(request.model().trim()); item.setSerialNumber(blankToNull(request.serialNumber()));
        item.setLocation(blankToNull(request.location())); item.setSupplier(request.supplier().trim()); item.setStatus(request.status());
        item.setPrice(request.price()); item.setCurrency(normalizeCurrency(request.currency())); item.setPurchaseDate(request.purchaseDate());
        item.setWarrantyEndDate(request.warrantyEndDate());
        if (request.shared() != null) item.setShared(request.shared());
        item.setUpdatedAt(Instant.now());
        return toResponse(item);
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public ItemResponse updateSharing(String owner, Long id, boolean shared) {
        HomeItemEntity item = requireAccessibleItem(owner, id);
        item.setShared(shared);
        item.setUpdatedAt(Instant.now());
        return toResponse(item);
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public void archive(String owner, Long id) {
        HomeItemEntity item = requireAccessibleItem(owner, id);
        if (item.getArchivedAt() != null) throw new IllegalArgumentException("El artículo ya está archivado");
        item.setArchivedAt(Instant.now());
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public ItemResponse restore(String owner, Long id) {
        HomeItemEntity item = requireAccessibleItem(owner, id);
        if (item.getArchivedAt() == null) throw new IllegalArgumentException("El artículo no está archivado");
        item.setArchivedAt(null);
        item.setUpdatedAt(Instant.now());
        return toResponse(item);
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public void deletePermanently(String owner, Long id) {
        HomeItemEntity item = requireAccessibleItem(owner, id);
        List<HomeAttachmentEntity> itemAttachments = attachments.findAllByItemIdAndOwnerEmailOrderByCreatedAtDesc(id, item.getOwnerEmail());
        items.delete(item);
        items.flush();
        for (HomeAttachmentEntity attachment : itemAttachments) {
            storage.delete(attachment.getStorageKey());
        }
    }

    @Transactional(transactionManager = "homeTransactionManager")
    public AttachmentResponse addAttachment(String owner, Long itemId, MultipartFile file, AttachmentType type) {
        HomeItemEntity item = requireAccessibleItem(owner, itemId);
        if (item.getArchivedAt() != null) throw new IllegalArgumentException("No puedes adjuntar documentos a un artículo archivado");
        return attachment(saveAttachment(item.getOwnerEmail(), itemId, file, type));
    }

    public HomeAttachmentEntity attachmentEntity(String owner, Long itemId, Long attachmentId) {
        HomeItemEntity item = requireAccessibleItem(owner, itemId);
        return attachments.findByIdAndItemIdAndOwnerEmail(attachmentId, itemId, item.getOwnerEmail()).orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
    }

    private HomeAttachmentEntity saveAttachment(String owner, Long itemId, MultipartFile file, AttachmentType type) {
        var stored = storage.store(file);
        return attachments.save(HomeAttachmentEntity.builder().itemId(itemId).ownerEmail(owner).documentType(type == null ? AttachmentType.OTHER : type)
                .originalFilename(stored.originalFilename()).storageKey(stored.key()).contentType(stored.contentType()).sizeBytes(stored.size()).createdAt(Instant.now()).build());
    }

    private ItemResponse toResponse(HomeItemEntity item) {
        var category = categories.findById(item.getCategoryId()).map(this::category).orElse(null);
        var files = attachments.findAllByItemIdAndOwnerEmailOrderByCreatedAtDesc(item.getId(), item.getOwnerEmail()).stream().map(this::attachment).toList();
        return new ItemResponse(item.getId(), item.getOwnerEmail(), item.isShared(), item.getName(), item.getDescription(), category, item.getBrand(), item.getModel(), item.getSerialNumber(),
                item.getLocation(), item.getSupplier(), item.getStatus(), item.getPrice(), item.getCurrency(), item.getPurchaseDate(), item.getWarrantyEndDate(),
                ChronoUnit.DAYS.between(LocalDate.now(), item.getWarrantyEndDate()), item.getArchivedAt() != null,
                files, item.getCreatedAt(), item.getUpdatedAt());
    }

    private AttachmentResponse attachment(HomeAttachmentEntity a) {
        return new AttachmentResponse(a.getId(), a.getDocumentType().name(), a.getOriginalFilename(), a.getContentType(), a.getSizeBytes(), a.getCreatedAt(),
                "/home/items/" + a.getItemId() + "/attachments/" + a.getId());
    }
    private CategoryResponse category(HomeCategoryEntity c) { return new CategoryResponse(c.getId(), c.getName(), c.getColor(), c.getIcon()); }
    private Set<String> accessibleOwners(String actor) {
        Set<String> owners = new LinkedHashSet<>();
        owners.add(actor);
        connections.overview(actor).connections().stream()
                .filter(connection -> connection.grantedByMe().getOrDefault("home", ModulePermission.NONE) == ModulePermission.ADMIN
                        || connection.grantedToMe().getOrDefault("home", ModulePermission.NONE) == ModulePermission.ADMIN)
                .map(connection -> connection.email())
                .forEach(owners::add);
        return owners;
    }
    private boolean scopeIsShared(String scope) { return "shared".equalsIgnoreCase(scope); }
    private HomeItemEntity requireAccessibleItem(String actor, Long id) {
        HomeItemEntity item = items.findById(id).orElseThrow(() -> new IllegalArgumentException("Artículo no encontrado"));
        if (item.getOwnerEmail().equalsIgnoreCase(actor)) return item;
        if (!item.isShared() || !hasHomeAccess(item.getOwnerEmail(), actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para gestionar este artículo");
        }
        return item;
    }
    private void requireCategoryAccess(String actor, String categoryOwner) {
        if (!categoryOwner.equalsIgnoreCase(actor) && !hasHomeAccess(categoryOwner, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para usar esta categoría");
        }
    }
    private void requireAdminOrOwner(String actor, String resourceOwner) {
        if (!resourceOwner.equalsIgnoreCase(actor) && !hasHomeAccess(resourceOwner, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para administrar este recurso");
        }
    }
    private boolean hasHomeAccess(String firstUser, String secondUser) {
        return connections.hasPermission(firstUser, secondUser, "home", ModulePermission.ADMIN)
                || connections.hasPermission(secondUser, firstUser, "home", ModulePermission.ADMIN);
    }
    private void validateDates(ItemRequest r) { if (r.warrantyEndDate().isBefore(r.purchaseDate())) throw new IllegalArgumentException("La garantía no puede terminar antes de la compra"); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalizeCurrency(String value) { return "EUR"; }
    private String categoryKey(String name) { return name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT); }
}
