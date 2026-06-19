package com.torresj.community.repositories;

import com.torresj.community.dtos.SearchResultDto;
import com.torresj.community.entities.ReportEntity;
import com.torresj.community.entities.ReportItemEntity;
import com.torresj.community.enums.ItemTypeEnum;
import com.torresj.community.enums.MeetingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReportItemSearchTest {

    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ReportItemRepository reportItemRepository;

    private long meeting(long communityId, String title) {
        return reportRepository.save(ReportEntity.builder()
                .communityId(communityId).title(title).status(MeetingStatus.HELD).build()).getId();
    }

    private void item(long reportId, int order, String description, String notes) {
        reportItemRepository.save(ReportItemEntity.builder()
                .reportId(reportId).itemOrder(order).description(description).notes(notes)
                .type(ItemTypeEnum.VOTING).build());
    }

    @Test
    void searchMatchesDescriptionAndNotesScopedToCommunities() {
        long m1 = meeting(1L, "Junta A");
        long m2 = meeting(2L, "Junta B");
        item(m1, 8, "Instalación de toldos en los bajos", "Sometido a votación se aprobó");
        item(m1, 5, "Estado de la azotea", "Incidencia de aerotermia");
        item(m2, 1, "Toldos en otra comunidad", null);

        // search for "toldos" within community 1 only
        var results = reportItemRepository.search("%toldos%", List.of(1L));
        assertThat(results).hasSize(1);
        SearchResultDto match = results.getFirst();
        assertThat(match.meetingId()).isEqualTo(m1);
        assertThat(match.itemOrder()).isEqualTo(8);
        assertThat(match.itemDescription()).contains("toldos");

        // notes match (aerotermia) within community 1
        assertThat(reportItemRepository.search("%aerotermia%", List.of(1L))).hasSize(1);

        // across both communities, "toldos" matches twice
        assertThat(reportItemRepository.search("%toldos%", List.of(1L, 2L))).hasSize(2);

        // community 2 only excludes community 1 results
        assertThat(reportItemRepository.search("%azotea%", List.of(2L))).isEmpty();
    }
}
