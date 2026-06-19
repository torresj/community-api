package com.torresj.community.controllers;

import com.torresj.community.dtos.AttendanceDto;
import com.torresj.community.dtos.ReportDto;
import com.torresj.community.dtos.ReportItemDto;
import com.torresj.community.dtos.RequestAttendanceDto;
import com.torresj.community.dtos.RequestAttendeesDto;
import com.torresj.community.dtos.RequestItemDto;
import com.torresj.community.dtos.RequestNewMeetingDto;
import com.torresj.community.dtos.RequestUpdateMeetingDto;
import com.torresj.community.dtos.RequestVotingDto;
import com.torresj.community.dtos.VotingDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.ReportItemNotFoundException;
import com.torresj.community.exceptions.ReportNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.exceptions.UserNotInCommunityException;
import com.torresj.community.security.AccessService;
import com.torresj.community.services.ActaImportService;
import com.torresj.community.services.AttendanceService;
import com.torresj.community.services.FileStorageService;
import com.torresj.community.services.ReportService;
import com.torresj.community.services.VotingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/v1/meetings")
@Slf4j
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final VotingService votingService;
    private final AttendanceService attendanceService;
    private final FileStorageService fileStorageService;
    private final ActaImportService actaImportService;
    private final AccessService accessService;

    // ---- meetings ----

    @Operation(summary = "Create a meeting (community admin or super admin)")
    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<ReportDto> create(@RequestBody RequestNewMeetingDto request, Principal principal)
            throws CommunityNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), request.communityId());
        return ResponseEntity.ok(reportService.create(
                request.communityId(), request.dateTime(), request.type(), request.status(),
                request.title(), request.location(), request.convocatoria(),
                request.presidentName(), request.secretaryName()));
    }

    @Operation(summary = "List meetings of a community")
    @GetMapping("/community/{communityId}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<ReportDto>> getByCommunity(@PathVariable long communityId, Principal principal)
            throws UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanView(principal.getName(), communityId);
        return ResponseEntity.ok(reportService.getByCommunityId(communityId));
    }

    @Operation(summary = "Get a meeting by id")
    @GetMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ReportDto> getById(@PathVariable long id, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        ReportDto report = reportService.get(id);
        accessService.assertCanView(principal.getName(), report.communityId());
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Update a meeting (community admin or super admin)")
    @PatchMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<ReportDto> update(
            @PathVariable long id, @RequestBody RequestUpdateMeetingDto request, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), reportService.get(id).communityId());
        return ResponseEntity.ok(reportService.update(
                id, request.dateTime(), request.type(), request.status(), request.title(),
                request.location(), request.convocatoria(), request.presidentName(), request.secretaryName()));
    }

    @Operation(summary = "Delete a meeting (community admin or super admin)")
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable long id, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), reportService.get(id).communityId());
        reportService.delete(id);
        return ResponseEntity.ok().build();
    }

    // ---- attendance ----

    @Operation(summary = "Add attendee properties to a meeting")
    @PostMapping("/{id}/attendees")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<ReportDto> addAttendees(
            @PathVariable long id, @RequestBody RequestAttendeesDto request, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), reportService.get(id).communityId());
        return ResponseEntity.ok(reportService.addAttendees(id, request.propertyIds()));
    }

    @Operation(summary = "Remove attendee properties from a meeting")
    @DeleteMapping("/{id}/attendees")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<ReportDto> removeAttendees(
            @PathVariable long id, @RequestBody RequestAttendeesDto request, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), reportService.get(id).communityId());
        return ResponseEntity.ok(reportService.removeAttendees(id, request.propertyIds()));
    }

    // ---- agenda items ----

    @Operation(summary = "List agenda items of a meeting")
    @GetMapping("/{id}/items")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<ReportItemDto>> getItems(@PathVariable long id, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanView(principal.getName(), reportService.get(id).communityId());
        return ResponseEntity.ok(reportService.getItems(id));
    }

    @Operation(summary = "Add an agenda item to a meeting")
    @PostMapping("/{id}/items")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<ReportItemDto> addItem(
            @PathVariable long id, @RequestBody RequestItemDto request, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), reportService.get(id).communityId());
        return ResponseEntity.ok(reportService.addItem(
                id, request.order(), request.description(), request.notes(), request.type()));
    }

    @Operation(summary = "Update an agenda item")
    @PatchMapping("/items/{itemId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<ReportItemDto> updateItem(
            @PathVariable long itemId, @RequestBody RequestItemDto request, Principal principal)
            throws ReportItemNotFoundException, ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        ReportItemDto item = reportService.getItem(itemId);
        accessService.assertCanManage(principal.getName(), reportService.get(item.reportId()).communityId());
        return ResponseEntity.ok(reportService.updateItem(
                itemId, request.order(), request.description(), request.notes(), request.type()));
    }

    @Operation(summary = "Remove an agenda item")
    @DeleteMapping("/items/{itemId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<Void> removeItem(@PathVariable long itemId, Principal principal)
            throws ReportItemNotFoundException, ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        ReportItemDto item = reportService.getItem(itemId);
        accessService.assertCanManage(principal.getName(), reportService.get(item.reportId()).communityId());
        reportService.removeItem(itemId);
        return ResponseEntity.ok().build();
    }

    // ---- voting (per agenda item) ----

    private long communityIdOfItem(long itemId) throws ReportItemNotFoundException, ReportNotFoundException {
        return reportService.get(reportService.getItem(itemId).reportId()).communityId();
    }

    @Operation(summary = "Create or replace the voting result of an agenda item")
    @PostMapping("/items/{itemId}/voting")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<VotingDto> upsertVoting(
            @PathVariable long itemId, @RequestBody RequestVotingDto request, Principal principal)
            throws ReportItemNotFoundException, ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), communityIdOfItem(itemId));
        return ResponseEntity.ok(votingService.upsertForItem(
                itemId, request.inFavorCount(), request.againstCount(), request.abstentionCount(),
                request.result(), request.unanimous(), request.listOfYes(), request.listOfNo(), request.abstentions()));
    }

    @Operation(summary = "Get the voting result of an agenda item")
    @GetMapping("/items/{itemId}/voting")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<VotingDto> getVoting(@PathVariable long itemId, Principal principal)
            throws ReportItemNotFoundException, ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanView(principal.getName(), communityIdOfItem(itemId));
        return ResponseEntity.ok(votingService.getByItem(itemId));
    }

    @Operation(summary = "Delete the voting result of an agenda item")
    @DeleteMapping("/items/{itemId}/voting")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<Void> deleteVoting(@PathVariable long itemId, Principal principal)
            throws ReportItemNotFoundException, ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), communityIdOfItem(itemId));
        votingService.deleteForItem(itemId);
        return ResponseEntity.ok().build();
    }

    // ---- import an acta PDF (auto-fill a DRAFT meeting) ----

    @Operation(summary = "Import an acta PDF: stores it and auto-creates a DRAFT meeting for review")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<ReportDto> importPdf(
            @RequestParam long communityId, @RequestParam("file") MultipartFile file, Principal principal)
            throws CommunityNotFoundException, ReportNotFoundException, ReportItemNotFoundException,
            UserNotFoundException, UserNotInCommunityException, IOException {
        accessService.assertCanManage(principal.getName(), communityId);
        return ResponseEntity.ok(actaImportService.importPdf(communityId, file.getBytes(), file.getOriginalFilename()));
    }

    // ---- original signed PDF ----

    @Operation(summary = "Upload the original signed minutes PDF for a meeting")
    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<ReportDto> uploadPdf(
            @PathVariable long id, @RequestParam("file") MultipartFile file, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException, IOException {
        accessService.assertCanManage(principal.getName(), reportService.get(id).communityId());
        String path = fileStorageService.store(file.getBytes(), file.getOriginalFilename());
        return ResponseEntity.ok(reportService.attachPdf(id, path));
    }

    @Operation(summary = "Download the original signed minutes PDF of a meeting")
    @GetMapping("/{id}/pdf")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Resource> downloadPdf(@PathVariable long id, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        ReportDto report = reportService.get(id);
        accessService.assertCanView(principal.getName(), report.communityId());
        if (report.pdfPath() == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = fileStorageService.load(report.pdfPath());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"acta.pdf\"")
                .body(resource);
    }

    // ---- detailed attendance (per property, with proxy) ----

    @Operation(summary = "Add a detailed attendance record to a meeting")
    @PostMapping("/{id}/attendance")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<AttendanceDto> addAttendance(
            @PathVariable long id, @RequestBody RequestAttendanceDto request, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), reportService.get(id).communityId());
        return ResponseEntity.ok(attendanceService.add(
                id, request.propertyId(), request.status(), request.representedBy()));
    }

    @Operation(summary = "List detailed attendance of a meeting")
    @GetMapping("/{id}/attendance")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<AttendanceDto>> getAttendance(@PathVariable long id, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanView(principal.getName(), reportService.get(id).communityId());
        return ResponseEntity.ok(attendanceService.getByReport(id));
    }

    @Operation(summary = "Delete a detailed attendance record")
    @DeleteMapping("/{id}/attendance/{attendanceId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<Void> deleteAttendance(
            @PathVariable long id, @PathVariable long attendanceId, Principal principal)
            throws ReportNotFoundException, UserNotFoundException, UserNotInCommunityException {
        accessService.assertCanManage(principal.getName(), reportService.get(id).communityId());
        attendanceService.delete(attendanceId);
        return ResponseEntity.ok().build();
    }
}
