package com.torresj.community.repositories;

import com.torresj.community.dtos.SearchResultDto;
import com.torresj.community.entities.ReportItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportItemRepository extends JpaRepository<ReportItemEntity, Long> {
    List<ReportItemEntity> findByReportId(Long reportId);

    List<ReportItemEntity> findByReportIdOrderByItemOrderAsc(Long reportId);

    /**
     * Case-insensitive search over agenda-item text (description + notes), scoped to a set of
     * communities, returning which meeting and item each topic was addressed in. Joins items to
     * their report on the manual FK ({@code i.reportId = r.id}). {@code q} must already be
     * lowercased and wrapped in {@code %...%}.
     */
    @Query("""
            SELECT new com.torresj.community.dtos.SearchResultDto(
                r.id, r.communityId, r.title, r.dateTime, i.id, i.itemOrder, i.description)
            FROM ReportItemEntity i, ReportEntity r
            WHERE i.reportId = r.id
              AND r.communityId IN :communityIds
              AND (LOWER(i.description) LIKE :q OR (i.notes IS NOT NULL AND LOWER(i.notes) LIKE :q))
            ORDER BY r.dateTime DESC, i.itemOrder ASC
            """)
    List<SearchResultDto> search(@Param("q") String q, @Param("communityIds") List<Long> communityIds);
}
