package com.torresj.community.controllers;

import com.torresj.community.dtos.RequestNewUserDto;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/v1/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Get users")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))
                            }),
            })
    @GetMapping()
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<UserDto>> get(Principal principal) throws CommunityNotFoundException {
        log.info("Getting Users for user {}", principal.getName());
        List<UserDto> users = userService.get();
        log.info("Users found: {}", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get user by ID")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User found",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = UserDto.class))
                            }),
                    @ApiResponse(responseCode = "404", description = "Not found", content = @Content),
            })
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<UserDto> getUserById(@Parameter(description = "User id") @PathVariable long id)
            throws UserNotFoundException, CommunityNotFoundException {
        log.info("Getting user by id {}", id);
        var user = userService.get(id);
        log.info("User found");
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get user by ID")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User found",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = UserDto.class))
                            }),
                    @ApiResponse(responseCode = "404", description = "Not found", content = @Content),
            })
    ResponseEntity<UserDto> getUserLogged(Principal principal)
            throws UserNotFoundException, CommunityNotFoundException {
        log.info("Getting user by name {}", principal.getName());
        var user = userService.get(principal.getName());
        log.info("User found");
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "create new user")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Created",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = UserDto.class))
                            }),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @ApiResponse(responseCode = "400", description = "Community Id not found", content = @Content)
            })
    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<UserDto> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New user",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RequestNewUserDto.class)))
            @RequestBody RequestNewUserDto request) throws CommunityNotFoundException {
        log.info("Creating new user {}", request);
        UserDto user = userService.create(
                request.communityId(),
                request.name(),
                passwordEncoder.encode(request.password()), request.role()
        );
        log.info("User created {}", user);
        return ResponseEntity.ok(user);
    }
}
