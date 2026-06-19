package com.torresj.community.controllers;

import com.torresj.community.dtos.ReportDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.MembershipRepository;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.torresj.community.enums.CommunityRole.ADMIN;
import static com.torresj.community.enums.CommunityRole.MEMBER;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ActaImportControllerTest {

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
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${admin.name}")
    private String adminName;

    private String importUri(long communityId) {
        return "http://localhost:" + port + "/v1/meetings/import?communityId=" + communityId;
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

    private HttpEntity<MultiValueMap<String, Object>> multipart(String username) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource("%PDF-1.4 fake acta".getBytes()) {
            @Override
            public String getFilename() {
                return "09032026 ORDINARIA.pdf";
            }
        });
        HttpHeaders headers = auth(username);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(form, headers);
    }

    @AfterEach
    void cleanUp() {
        reportRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(adminName))
                .forEach(userRepository::delete);
        communityRepository.deleteAll();
    }

    @Test
    public void givenAdmin_WhenImportPdf_ThenDraftMeetingCreatedAndPdfStored() {
        // With no ANTHROPIC_API_KEY in the test profile, the NoOp extractor yields an empty draft,
        // but the PDF is still stored and a DRAFT meeting is created for the admin to complete.
        long communityId = communityWithAdmin("importCommA", "importAdmin");

        var result = restTemplate.exchange(importUri(communityId), POST, multipart("importAdmin"), ReportDto.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().status()).isEqualTo(MeetingStatus.DRAFT);
        assertThat(result.getBody().communityId()).isEqualTo(communityId);
        assertThat(result.getBody().pdfPath()).isNotNull();
    }

    @Test
    public void givenPlainMember_WhenImportPdf_ThenForbidden() {
        var c = communityRepository.save(CommunityEntity.builder().name("importCommB").description("d").build());
        var user = userRepository.save(UserEntity.builder()
                .username("importMember").password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(c.getId()).role(MEMBER).build());

        var result = restTemplate.exchange(importUri(c.getId()), POST, multipart("importMember"), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
