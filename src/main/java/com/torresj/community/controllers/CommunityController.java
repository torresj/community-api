package com.torresj.community.controllers;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.RequestNewCommunityDto;
import com.torresj.community.dtos.RequestUpdateCommunityDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.services.CommunityService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/v1/communities")
@Slf4j
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final UserService userService;

    @Operation(summary = "Get all communities")
    @ApiResponses(
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CommunityDto.class)))))
    @GetMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<List<CommunityDto>> get() {
        log.info("Getting all communities");
        return ResponseEntity.ok(communityService.get());
    }

    @Operation(summary = "Get communities of the logged user")
    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<CommunityDto>> getMine(Principal principal)
            throws UserNotFoundException, CommunityNotFoundException {
        log.info("Getting communities for user {}", principal.getName());
        var user = userService.getEntity(principal.getName());
        return ResponseEntity.ok(communityService.getByUserId(user.getId()));
    }

    @Operation(summary = "Get community by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommunityDto.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @GetMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<CommunityDto> getById(@Parameter(description = "Community id") @PathVariable long id)
            throws CommunityNotFoundException {
        log.info("Getting community {}", id);
        return ResponseEntity.ok(communityService.get(id));
    }

    @Operation(summary = "Create community (super admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommunityDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<CommunityDto> create(@RequestBody RequestNewCommunityDto request) {
        log.info("Creating community {}", request.name());
        return ResponseEntity.ok(
                communityService.create(request.name(), request.description(), request.address(), request.cif()));
    }

    @Operation(summary = "Update community (super admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommunityDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PatchMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<CommunityDto> update(
            @Parameter(description = "Community id") @PathVariable long id,
            @RequestBody RequestUpdateCommunityDto request) throws CommunityNotFoundException {
        log.info("Updating community {}", id);
        return ResponseEntity.ok(
                communityService.update(id, request.description(), request.address(), request.cif()));
    }

    @Operation(summary = "Delete community (super admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Void> delete(@Parameter(description = "Community id") @PathVariable long id) {
        log.info("Deleting community {}", id);
        communityService.delete(id);
        return ResponseEntity.ok().build();
    }
}
