package com.torresj.community.controllers;

import com.torresj.community.dtos.ReportDto;
import com.torresj.community.dtos.ReportItemDto;
import com.torresj.community.dtos.RequestAttendeesDto;
import com.torresj.community.dtos.RequestItemDto;
import com.torresj.community.dtos.RequestNewMeetingDto;
import com.torresj.community.dtos.RequestUpdateMeetingDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.CommunityRole;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.torresj.community.enums.CommunityRole.ADMIN;
import static com.torresj.community.enums.ItemTypeEnum.VOTING;
import static com.torresj.community.enums.MeetingStatus.SCHEDULED;
import static com.torresj.community.enums.MeetingType.ORDINARIA;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReportControllerTest {

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

    private String baseUri() {
        return "http://localhost:" + port + "/v1/meetings";
    }

    private HttpHeaders auth(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.createJWS(username));
        return headers;
    }

    private CommunityEntity community(String name) {
        return communityRepository.save(CommunityEntity.builder().name(name).description("d").build());
    }

    private void userWithMembership(String username, long communityId, CommunityRole role) {
        var user = userRepository.save(UserEntity.builder()
                .username(username).password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(communityId).role(role).build());
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
    public void givenAdmin_WhenCreateMeeting_ThenCreated() {
        var c = community("meetCommA");
        userWithMembership("meetAdmin", c.getId(), ADMIN);
        var request = new RequestNewMeetingDto(c.getId(),
                LocalDateTime.of(2026, 3, 9, 17, 30), ORDINARIA, SCHEDULED,
                "Junta Ordinaria", "Salón social", 2, "Presidente", "Secretario");

        var result = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(request, auth("meetAdmin")), ReportDto.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().type()).isEqualTo(ORDINARIA);
        assertThat(result.getBody().convocatoria()).isEqualTo(2);
        assertThat(result.getBody().items()).isEmpty();
    }

    @Test
    public void givenNonAdmin_WhenCreateMeeting_ThenForbidden() {
        var c = community("meetCommB");
        userWithMembership("meetMember", c.getId(), CommunityRole.MEMBER);
        var request = new RequestNewMeetingDto(c.getId(), null, ORDINARIA, SCHEDULED,
                "t", null, null, null, null);

        var result = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(request, auth("meetMember")), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void givenMeeting_WhenAddItemsAndList_ThenOrderedReturned() {
        var c = community("meetCommC");
        userWithMembership("meetAdminC", c.getId(), ADMIN);
        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(c.getId(), null, ORDINARIA, SCHEDULED,
                        "t", null, null, null, null), auth("meetAdminC")), ReportDto.class).getBody();

        restTemplate.exchange(baseUri() + "/" + meeting.id() + "/items", POST,
                new HttpEntity<>(new RequestItemDto(2, "second", null, VOTING), auth("meetAdminC")), ReportItemDto.class);
        restTemplate.exchange(baseUri() + "/" + meeting.id() + "/items", POST,
                new HttpEntity<>(new RequestItemDto(1, "first", "note", VOTING), auth("meetAdminC")), ReportItemDto.class);

        var items = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/items", GET,
                new HttpEntity<>(auth("meetAdminC")), ReportItemDto[].class).getBody();

        assertThat(items).hasSize(2);
        assertThat(items[0].order()).isEqualTo(1);
        assertThat(items[0].description()).isEqualTo("first");
        assertThat(items[1].order()).isEqualTo(2);
    }

    @Test
    public void givenMeeting_WhenAddAttendees_ThenStored() {
        var c = community("meetCommD");
        userWithMembership("meetAdminD", c.getId(), ADMIN);
        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(c.getId(), null, ORDINARIA, SCHEDULED,
                        "t", null, null, null, null), auth("meetAdminD")), ReportDto.class).getBody();

        var result = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/attendees", POST,
                new HttpEntity<>(new RequestAttendeesDto(List.of(10L, 20L)), auth("meetAdminD")), ReportDto.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().attendeesPropertyIds()).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    public void givenMeeting_WhenUpdateStatusToHeld_ThenUpdated() {
        var c = community("meetCommE");
        userWithMembership("meetAdminE", c.getId(), ADMIN);
        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(c.getId(), null, ORDINARIA, SCHEDULED,
                        "t", null, null, null, null), auth("meetAdminE")), ReportDto.class).getBody();

        var update = new RequestUpdateMeetingDto(null, null, com.torresj.community.enums.MeetingStatus.HELD,
                null, null, null, null, null);
        var result = restTemplate.exchange(baseUri() + "/" + meeting.id(), PATCH,
                new HttpEntity<>(update, auth("meetAdminE")), ReportDto.class);

        assertThat(result.getBody().status()).isEqualTo(com.torresj.community.enums.MeetingStatus.HELD);
    }

    @Test
    public void givenMeeting_WhenDelete_ThenRemoved() {
        var c = community("meetCommF");
        userWithMembership("meetAdminF", c.getId(), ADMIN);
        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(c.getId(), null, ORDINARIA, SCHEDULED,
                        "t", null, null, null, null), auth("meetAdminF")), ReportDto.class).getBody();

        restTemplate.exchange(baseUri() + "/" + meeting.id(), DELETE,
                new HttpEntity<>(auth("meetAdminF")), Void.class);

        assertThat(reportRepository.findById(meeting.id())).isEmpty();
    }

    @Test
    public void givenMissingMeeting_WhenGetById_ThenNotFound() {
        var result = restTemplate.exchange(baseUri() + "/99999", GET,
                new HttpEntity<>(auth(adminName)), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().getTitle()).isEqualTo("Meeting 99999 not found");
    }

    @Test
    public void givenNonMember_WhenListCommunityMeetings_ThenForbidden() {
        var c = community("meetCommG");
        var other = community("meetCommH");
        userWithMembership("meetOutsider", other.getId(), CommunityRole.MEMBER);

        var result = restTemplate.exchange(baseUri() + "/community/" + c.getId(), GET,
                new HttpEntity<>(auth("meetOutsider")), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
