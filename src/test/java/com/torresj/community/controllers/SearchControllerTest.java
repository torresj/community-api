package com.torresj.community.controllers;

import com.torresj.community.dtos.SearchResultDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.ReportEntity;
import com.torresj.community.entities.ReportItemEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.ItemTypeEnum;
import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.ReportItemRepository;
import com.torresj.community.repositories.ReportRepository;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.services.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static com.torresj.community.enums.CommunityRole.MEMBER;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SearchControllerTest {

    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private CommunityRepository communityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ReportItemRepository reportItemRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${admin.name}")
    private String adminName;

    private String uri(String q, Long communityId) {
        String u = "http://localhost:" + port + "/v1/search?q=" + q;
        return communityId == null ? u : u + "&communityId=" + communityId;
    }

    private HttpHeaders auth(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.createJWS(username));
        return headers;
    }

    private long meeting(long communityId, String title) {
        return reportRepository.save(ReportEntity.builder()
                .communityId(communityId).title(title).status(MeetingStatus.HELD).build()).getId();
    }

    private void item(long reportId, int order, String description) {
        reportItemRepository.save(ReportItemEntity.builder()
                .reportId(reportId).itemOrder(order).description(description).type(ItemTypeEnum.VOTING).build());
    }

    @AfterEach
    void cleanUp() {
        reportItemRepository.deleteAll();
        reportRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(adminName))
                .forEach(userRepository::delete);
        communityRepository.deleteAll();
    }

    @Test
    public void givenMemberOfCommunity_WhenSearch_ThenOnlyTheirCommunityMatches() {
        var a = communityRepository.save(CommunityEntity.builder().name("searchA").description("d").build());
        var b = communityRepository.save(CommunityEntity.builder().name("searchB").description("d").build());
        long mA = meeting(a.getId(), "Junta A");
        long mB = meeting(b.getId(), "Junta B");
        item(mA, 8, "Instalación de toldos");
        item(mB, 1, "Toldos en B");
        var user = userRepository.save(UserEntity.builder()
                .username("searchUser").password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(a.getId()).role(MEMBER).build());

        var result = restTemplate.exchange(uri("toldos", null), GET,
                new HttpEntity<>(auth("searchUser")), SearchResultDto[].class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody()[0].meetingId()).isEqualTo(mA);
    }

    @Test
    public void givenSuperAdmin_WhenSearch_ThenAllCommunitiesMatch() {
        var a = communityRepository.save(CommunityEntity.builder().name("searchC").description("d").build());
        var b = communityRepository.save(CommunityEntity.builder().name("searchD").description("d").build());
        item(meeting(a.getId(), "A"), 1, "Reparación ascensor");
        item(meeting(b.getId(), "B"), 1, "Ascensor mantenimiento");

        var result = restTemplate.exchange(uri("ascensor", null), GET,
                new HttpEntity<>(auth(adminName)), SearchResultDto[].class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
    }

    @Test
    public void givenNonMember_WhenSearchScopedToCommunity_ThenForbidden() {
        var a = communityRepository.save(CommunityEntity.builder().name("searchE").description("d").build());
        var other = communityRepository.save(CommunityEntity.builder().name("searchF").description("d").build());
        var user = userRepository.save(UserEntity.builder()
                .username("searchOutsider").password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(other.getId()).role(MEMBER).build());

        var result = restTemplate.exchange(uri("x", a.getId()), GET,
                new HttpEntity<>(auth("searchOutsider")), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
