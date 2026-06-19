package com.torresj.community.controllers;

import com.torresj.community.dtos.AttendanceDto;
import com.torresj.community.dtos.ReportDto;
import com.torresj.community.dtos.ReportItemDto;
import com.torresj.community.dtos.RequestAttendanceDto;
import com.torresj.community.dtos.RequestItemDto;
import com.torresj.community.dtos.RequestNewMeetingDto;
import com.torresj.community.dtos.RequestVotingDto;
import com.torresj.community.dtos.VotingDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.repositories.AttendanceRepository;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.ReportItemRepository;
import com.torresj.community.repositories.ReportRepository;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.repositories.VotingRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.torresj.community.enums.AttendanceStatus.REPRESENTED;
import static com.torresj.community.enums.CommunityRole.ADMIN;
import static com.torresj.community.enums.ItemTypeEnum.VOTING;
import static com.torresj.community.enums.MeetingStatus.HELD;
import static com.torresj.community.enums.MeetingType.ORDINARIA;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static com.torresj.community.enums.VotingResult.APPROVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class VotingAttendanceControllerTest {

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
    private VotingRepository votingRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
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

    private long communityWithAdmin(String name, String admin) {
        var c = communityRepository.save(CommunityEntity.builder().name(name).description("d").build());
        var user = userRepository.save(UserEntity.builder()
                .username(admin).password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(c.getId()).role(ADMIN).build());
        return c.getId();
    }

    @AfterEach
    void cleanUp() {
        attendanceRepository.deleteAll();
        reportItemRepository.deleteAll();
        votingRepository.deleteAll();
        reportRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(adminName))
                .forEach(userRepository::delete);
        communityRepository.deleteAll();
    }

    @Test
    public void givenItem_WhenUpsertVotingWithCounts_ThenAttachedToMeeting() {
        long communityId = communityWithAdmin("voteCommA", "voteAdminA");
        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(communityId, null, ORDINARIA, HELD,
                        "t", null, null, null, null), auth("voteAdminA")), ReportDto.class).getBody();
        var item = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/items", POST,
                new HttpEntity<>(new RequestItemDto(8, "Toldos", null, VOTING), auth("voteAdminA")),
                ReportItemDto.class).getBody();

        var votingRequest = new RequestVotingDto(15, 1, 9, APPROVED, false, null, null, null);
        var votingResult = restTemplate.exchange(baseUri() + "/items/" + item.id() + "/voting", POST,
                new HttpEntity<>(votingRequest, auth("voteAdminA")), VotingDto.class);

        assertThat(votingResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(votingResult.getBody().inFavorCount()).isEqualTo(15);
        assertThat(votingResult.getBody().result()).isEqualTo(APPROVED);

        // The meeting now exposes the voting on its agenda item.
        var fetched = restTemplate.exchange(baseUri() + "/" + meeting.id(), GET,
                new HttpEntity<>(auth("voteAdminA")), ReportDto.class).getBody();
        assertThat(fetched.items()).hasSize(1);
        assertThat(fetched.items().getFirst().voting()).isNotNull();
        assertThat(fetched.items().getFirst().voting().againstCount()).isEqualTo(1);
    }

    @Test
    public void givenItem_WhenUpsertUnanimousVoting_ThenStored() {
        long communityId = communityWithAdmin("voteCommB", "voteAdminB");
        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(communityId, null, ORDINARIA, HELD,
                        "t", null, null, null, null), auth("voteAdminB")), ReportDto.class).getBody();
        var item = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/items", POST,
                new HttpEntity<>(new RequestItemDto(1, "Balance", null, VOTING), auth("voteAdminB")),
                ReportItemDto.class).getBody();

        var votingRequest = new RequestVotingDto(null, null, null, APPROVED, true, null, null, null);
        var votingResult = restTemplate.exchange(baseUri() + "/items/" + item.id() + "/voting", POST,
                new HttpEntity<>(votingRequest, auth("voteAdminB")), VotingDto.class);

        assertThat(votingResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(votingResult.getBody().unanimous()).isTrue();
    }

    @Test
    public void givenMeeting_WhenAddDetailedAttendanceWithProxy_ThenListed() {
        long communityId = communityWithAdmin("voteCommC", "voteAdminC");
        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(communityId, null, ORDINARIA, HELD,
                        "t", null, null, null, null), auth("voteAdminC")), ReportDto.class).getBody();

        var request = new RequestAttendanceDto(42L, REPRESENTED, "D. Bruno Ortega");
        var added = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/attendance", POST,
                new HttpEntity<>(request, auth("voteAdminC")), AttendanceDto.class);

        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(added.getBody().representedBy()).isEqualTo("D. Bruno Ortega");

        var list = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/attendance", GET,
                new HttpEntity<>(auth("voteAdminC")), AttendanceDto[].class).getBody();
        assertThat(List.of(list)).hasSize(1);
        assertThat(list[0].status()).isEqualTo(REPRESENTED);
    }
}
