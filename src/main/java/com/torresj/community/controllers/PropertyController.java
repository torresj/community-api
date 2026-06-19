package com.torresj.community.controllers;

import com.torresj.community.dtos.PropertyDto;
import com.torresj.community.dtos.RequestNewPropertyDto;
import com.torresj.community.dtos.RequestUpdatePropertyDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.PropertyNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.exceptions.UserNotInCommunityException;
import com.torresj.community.security.AccessService;
import com.torresj.community.services.PropertyService;
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
@RequestMapping("/v1/properties")
@Slf4j
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final AccessService accessService;

    @Operation(summary = "Get properties owned by the logged user")
    @ApiResponses(
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PropertyDto.class)))))
    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<PropertyDto>> getMine(Principal principal) throws UserNotFoundException {
        log.info("Getting properties for user {}", principal.getName());
        var user = accessService.requireUser(principal.getName());
        return ResponseEntity.ok(propertyService.getByUserId(user.getId()));
    }

    @Operation(summary = "Get properties of a community")
    @GetMapping("/community/{communityId}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<PropertyDto>> getByCommunity(
            @Parameter(description = "Community id") @PathVariable long communityId, Principal principal)
            throws UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanView(principal.getName(), communityId);
        return ResponseEntity.ok(propertyService.getByCommunityId(communityId));
    }

    @Operation(summary = "Get property by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropertyDto.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @GetMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PropertyDto> getById(@Parameter(description = "Property id") @PathVariable long id, Principal principal)
            throws PropertyNotFoundException, UserNotFoundException, UserNotInCommunityException {
        var property = propertyService.get(id);
        accessService.assertCanView(principal.getName(), property.communityId());
        return ResponseEntity.ok(property);
    }

    @Operation(summary = "Create property (community admin or super admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropertyDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Community not found", content = @Content)
    })
    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<PropertyDto> create(@RequestBody RequestNewPropertyDto request, Principal principal)
            throws CommunityNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), request.communityId());
        log.info("Creating property {} in community {}", request.code(), request.communityId());
        return ResponseEntity.ok(
                propertyService.create(
                        request.userId(), request.communityId(), request.code(),
                        request.coefficient(), request.description()));
    }

    @Operation(summary = "Update property (community admin or super admin)")
    @PatchMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<PropertyDto> update(
            @Parameter(description = "Property id") @PathVariable long id,
            @RequestBody RequestUpdatePropertyDto request, Principal principal)
            throws PropertyNotFoundException, UserNotFoundException, UserNotInCommunityException {
        var property = propertyService.get(id);
        accessService.assertCanManage(principal.getName(), property.communityId());
        return ResponseEntity.ok(propertyService.update(id, request.userId(), request.description()));
    }

    @Operation(summary = "Delete property (community admin or super admin)")
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<Void> delete(@Parameter(description = "Property id") @PathVariable long id, Principal principal)
            throws PropertyNotFoundException, UserNotFoundException, UserNotInCommunityException {
        var property = propertyService.get(id);
        accessService.assertCanManage(principal.getName(), property.communityId());
        propertyService.delete(id);
        return ResponseEntity.ok().build();
    }
}
