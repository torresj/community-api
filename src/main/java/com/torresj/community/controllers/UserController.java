package com.torresj.community.controllers;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<UserDto> getUserLogged(Principal principal)
            throws UserNotFoundException, CommunityNotFoundException {
        log.info("Getting user by name {}", principal.getName());
        var user = userService.get(principal.getName());
        log.info("User found");
        return ResponseEntity.ok(user);
    }
}
