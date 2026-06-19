package com.torresj.community.controllers;

import com.torresj.community.dtos.PropertyDto;
import com.torresj.community.dtos.RequestNewPropertyDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.PropertyEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.CommunityRole;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.PropertyRepository;
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

import static com.torresj.community.enums.CommunityRole.ADMIN;
import static com.torresj.community.enums.CommunityRole.MEMBER;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PropertyControllerTest {

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
    private PropertyRepository propertyRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${admin.name}")
    private String adminName;

    private String baseUri() {
        return "http://localhost:" + port + "/v1/properties";
    }

    private HttpHeaders auth(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.createJWS(username));
        return headers;
    }

    private CommunityEntity community(String name) {
        return communityRepository.save(CommunityEntity.builder().name(name).description("d").build());
    }

    private UserEntity userWithMembership(String username, long communityId, CommunityRole role) {
        var user = userRepository.save(UserEntity.builder()
                .username(username).password(passwordEncoder.encode("p")).role(ROLE_USER).build());
        membershipRepository.save(MembershipEntity.builder()
                .userId(user.getId()).communityId(communityId).role(role).build());
        return user;
    }

    @AfterEach
    void cleanUp() {
        propertyRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(adminName))
                .forEach(userRepository::delete);
        communityRepository.deleteAll();
    }

    @Test
    public void givenSuperAdmin_WhenCreateProperty_ThenCreated() {
        var c = community("propCommA");
        var request = new RequestNewPropertyDto(null, c.getId(), "P.1-1ºA,AP.TR.7", 2.86, "Piso 1");

        var result = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(request, auth(adminName)), PropertyDto.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().code()).isEqualTo("P.1-1ºA,AP.TR.7");
        assertThat(result.getBody().coefficient()).isEqualTo(2.86);
    }

    @Test
    public void givenCommunityAdmin_WhenCreatePropertyInOwnCommunity_ThenCreated() {
        var c = community("propCommB");
        var admin = userWithMembership("propAdmin", c.getId(), ADMIN);
        var request = new RequestNewPropertyDto(admin.getId(), c.getId(), "P.2-1ºB", 2.37, null);

        var result = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(request, auth("propAdmin")), PropertyDto.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().userId()).isEqualTo(admin.getId());
    }

    @Test
    public void givenCommunityAdmin_WhenCreatePropertyInOtherCommunity_ThenForbidden() {
        var a = community("propCommC");
        var b = community("propCommD");
        userWithMembership("propAdmin2", a.getId(), ADMIN);
        var request = new RequestNewPropertyDto(null, b.getId(), "X", 1.0, null);

        var result = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(request, auth("propAdmin2")), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void givenPlainMember_WhenCreateProperty_ThenForbidden() {
        var c = community("propCommE");
        userWithMembership("plainMember", c.getId(), MEMBER);
        var request = new RequestNewPropertyDto(null, c.getId(), "X", 1.0, null);

        var result = restTemplate.exchange(baseUri(), POST,
                new HttpEntity<>(request, auth("plainMember")), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void givenMember_WhenGetCommunityProperties_ThenReturned() {
        var c = community("propCommF");
        userWithMembership("viewer", c.getId(), MEMBER);
        propertyRepository.save(PropertyEntity.builder()
                .communityId(c.getId()).code("P.1").coefficient(1.0).build());

        var result = restTemplate.exchange(baseUri() + "/community/" + c.getId(), GET,
                new HttpEntity<>(auth("viewer")), PropertyDto[].class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    public void givenNonMember_WhenGetCommunityProperties_ThenForbidden() {
        var c = community("propCommG");
        var other = community("propCommH");
        userWithMembership("outsider", other.getId(), MEMBER);

        var result = restTemplate.exchange(baseUri() + "/community/" + c.getId(), GET,
                new HttpEntity<>(auth("outsider")), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void givenUser_WhenGetMine_ThenReturnOwnProperties() {
        var c = community("propCommI");
        var user = userWithMembership("ownerUser", c.getId(), MEMBER);
        propertyRepository.save(PropertyEntity.builder()
                .userId(user.getId()).communityId(c.getId()).code("P.1").coefficient(1.0).build());

        var result = restTemplate.exchange(baseUri() + "/me", GET,
                new HttpEntity<>(auth("ownerUser")), PropertyDto[].class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody()[0].userId()).isEqualTo(user.getId());
    }

    @Test
    public void givenMissingProperty_WhenGetById_ThenNotFound() {
        var result = restTemplate.exchange(baseUri() + "/99999", GET,
                new HttpEntity<>(auth(adminName)), ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTitle()).isEqualTo("Property 99999 not found");
    }
}
