package com.torresj.community.controllers;

import com.torresj.community.dtos.ReportDto;
import com.torresj.community.dtos.RequestNewMeetingDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.UserEntity;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.torresj.community.enums.CommunityRole.ADMIN;
import static com.torresj.community.enums.MeetingStatus.HELD;
import static com.torresj.community.enums.MeetingType.ORDINARIA;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReportPdfControllerTest {

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

    private String baseUri() {
        return "http://localhost:" + port + "/v1/meetings";
    }

    private HttpHeaders auth(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.createJWS(username));
        return headers;
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
    public void givenMeeting_WhenUploadAndDownloadPdf_ThenRoundTrips() {
        var c = communityRepository.save(CommunityEntity.builder().name("pdfComm").description("d").build());
        var user = userRepository.save(UserEntity.builder()
                .username("pdfAdmin").password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(c.getId()).role(ADMIN).build());

        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(c.getId(), null, ORDINARIA, HELD,
                        "t", null, null, null, null), auth("pdfAdmin")), ReportDto.class).getBody();

        byte[] pdfBytes = "%PDF-1.4 fake acta".getBytes();
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return "acta.pdf";
            }
        });
        HttpHeaders uploadHeaders = auth("pdfAdmin");
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        var uploadResult = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/pdf", POST,
                new HttpEntity<>(form, uploadHeaders), ReportDto.class);

        assertThat(uploadResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploadResult.getBody().pdfPath()).isNotNull();

        var downloadResult = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/pdf", GET,
                new HttpEntity<>(auth("pdfAdmin")), byte[].class);

        assertThat(downloadResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResult.getBody()).isEqualTo(pdfBytes);
    }

    @Test
    public void givenMeetingWithoutPdf_WhenDownload_ThenNotFound() {
        var c = communityRepository.save(CommunityEntity.builder().name("pdfComm2").description("d").build());
        var user = userRepository.save(UserEntity.builder()
                .username("pdfAdmin2").password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(c.getId()).role(ADMIN).build());
        var meeting = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(new RequestNewMeetingDto(c.getId(), null, ORDINARIA, HELD,
                        "t", null, null, null, null), auth("pdfAdmin2")), ReportDto.class).getBody();

        var result = restTemplate.exchange(baseUri() + "/" + meeting.id() + "/pdf", GET,
                new HttpEntity<>(auth("pdfAdmin2")), byte[].class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
