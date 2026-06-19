package com.torresj.community.controllers;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.RequestNewCommunityDto;
import com.torresj.community.dtos.RequestUpdateCommunityDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.UserRole;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.services.JwtService;
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
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CommunityControllerTest {

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
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.name}")
    private String adminName;

    private String baseUri() {
        return "http://localhost:" + port + "/v1/communities";
    }

    private HttpHeaders auth(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.createJWS(username));
        return headers;
    }

    @Test
    public void givenSuperAdmin_WhenCreateCommunity_ThenItIsCreated() {
        var request = new RequestNewCommunityDto("Taraceas", "Fase C", "Granada", "H-72842750");
        var result = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(request, auth(adminName)), CommunityDto.class);
        var body = result.getBody();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("Taraceas");
        assertThat(body.cif()).isEqualTo("H-72842750");
        assertThat(body.properties()).isEmpty();

        communityRepository.deleteById(body.id());
    }

    @Test
    public void givenNonSuperAdmin_WhenCreateCommunity_ThenForbidden() {
        var user = userRepository.save(UserEntity.builder()
                .username("plainUser").password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        var request = new RequestNewCommunityDto("X", "d", null, null);

        var result = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(request, auth("plainUser")), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        userRepository.delete(user);
    }

    @Test
    public void givenCommunity_WhenGetById_ThenReturned() {
        var community = communityRepository.save(
                CommunityEntity.builder().name("getByIdComm").description("d").build());

        var result = restTemplate.exchange(baseUri() + "/" + community.getId(), GET,
                new HttpEntity<>(auth(adminName)), CommunityDto.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().name()).isEqualTo("getByIdComm");

        communityRepository.delete(community);
    }

    @Test
    public void givenMissingCommunity_WhenGetById_ThenNotFound() {
        var result = restTemplate.exchange(baseUri() + "/99999", GET,
                new HttpEntity<>(auth(adminName)), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTitle()).isEqualTo("Community 99999 not found");
    }

    @Test
    public void givenSuperAdmin_WhenUpdateCommunity_ThenUpdated() {
        var community = communityRepository.save(
                CommunityEntity.builder().name("updateComm").description("old").build());
        var request = new RequestUpdateCommunityDto("new desc", "new address", null);

        var result = restTemplate.exchange(baseUri() + "/" + community.getId(), PATCH,
                new HttpEntity<>(request, auth(adminName)), CommunityDto.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().description()).isEqualTo("new desc");
        assertThat(result.getBody().address()).isEqualTo("new address");

        communityRepository.delete(community);
    }

    @Test
    public void givenSuperAdmin_WhenDeleteCommunity_ThenDeleted() {
        var community = communityRepository.save(
                CommunityEntity.builder().name("deleteComm").description("d").build());

        restTemplate.exchange(baseUri() + "/" + community.getId(), DELETE,
                new HttpEntity<>(auth(adminName)), Void.class);

        assertThat(communityRepository.findById(community.getId())).isEmpty();
    }

    @Test
    public void givenUserWithMembership_WhenGetMine_ThenReturnTheirCommunities() {
        var community = communityRepository.save(
                CommunityEntity.builder().name("myComm").description("d").build());
        var user = userRepository.save(UserEntity.builder()
                .username("memberUser").password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        var membership = membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(community.getId()).role(MEMBER).build());

        var result = restTemplate.exchange(baseUri() + "/me", GET,
                new HttpEntity<>(auth("memberUser")), CommunityDto[].class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody()[0].id()).isEqualTo(community.getId());

        membershipRepository.delete(membership);
        userRepository.delete(user);
        communityRepository.delete(community);
    }
}
