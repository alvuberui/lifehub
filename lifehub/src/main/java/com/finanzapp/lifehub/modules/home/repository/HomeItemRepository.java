package com.finanzapp.lifehub.modules.home.repository;

import com.finanzapp.lifehub.modules.home.domain.HomeItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HomeItemRepository extends JpaRepository<HomeItemEntity, Long> {
    boolean existsByCategoryId(Long categoryId);
    Optional<HomeItemEntity> findByIdAndOwnerEmail(Long id, String ownerEmail);
    Optional<HomeItemEntity> findByIdAndOwnerEmailAndArchivedAtIsNull(Long id, String ownerEmail);
    List<HomeItemEntity> findAllByOwnerEmailAndArchivedAtIsNotNullOrderByArchivedAtDesc(String ownerEmail);
    Optional<HomeItemEntity> findByIdAndOwnerEmailAndArchivedAtIsNotNull(Long id, String ownerEmail);

    @Query("select i from HomeItemEntity i where i.ownerEmail in :owners and i.shared = :shared " +
            "and i.archivedAt is null " +
            "and (:query = '' or lower(i.name) like lower(concat('%', :query, '%')) " +
            "or lower(i.brand) like lower(concat('%', :query, '%')) " +
            "or lower(i.model) like lower(concat('%', :query, '%'))) " +
            "and (:categoryId is null or i.categoryId = :categoryId) " +
            "and (:status is null or i.status = :status) " +
            "order by i.warrantyEndDate asc, i.name asc")
    List<HomeItemEntity> searchByOwnersAndShared(@Param("owners") Collection<String> owners,
                                                  @Param("shared") boolean shared,
                                                  @Param("query") String query,
                                                  @Param("categoryId") Long categoryId,
                                                  @Param("status") HomeItemStatus status);

    @Query("select i from HomeItemEntity i where i.ownerEmail in :owners and i.shared = :shared " +
            "and i.archivedAt is not null order by i.archivedAt desc")
    List<HomeItemEntity> findArchivedByOwnersAndShared(@Param("owners") Collection<String> owners,
                                                        @Param("shared") boolean shared);

    @Query("select i from HomeItemEntity i where i.ownerEmail in :owners and i.shared = true " +
            "and i.archivedAt is null and i.warrantyEndDate between :today and :until " +
            "order by i.warrantyEndDate asc")
    List<HomeItemEntity> findSharedWarrantyExpiring(@Param("owners") Collection<String> owners,
                                                     @Param("today") LocalDate today,
                                                     @Param("until") LocalDate until);

    @Query("select i from HomeItemEntity i where i.ownerEmail = :owner and i.archivedAt is null " +
            "and (:query = '' or lower(i.name) like lower(concat('%', :query, '%')) " +
            "or lower(i.brand) like lower(concat('%', :query, '%')) " +
            "or lower(i.model) like lower(concat('%', :query, '%'))) " +
            "and (:categoryId is null or i.categoryId = :categoryId) " +
            "and (:status is null or i.status = :status) " +
            "order by i.warrantyEndDate asc, i.name asc")
    List<HomeItemEntity> search(@Param("owner") String ownerEmail,
                                @Param("query") String query,
                                @Param("categoryId") Long categoryId,
                                @Param("status") HomeItemStatus status);

    @Query("select i from HomeItemEntity i where i.ownerEmail = :owner and i.archivedAt is null " +
            "and i.warrantyEndDate between :today and :until order by i.warrantyEndDate asc")
    List<HomeItemEntity> findWarrantyExpiring(@Param("owner") String ownerEmail,
                                              @Param("today") LocalDate today,
                                              @Param("until") LocalDate until);
}
