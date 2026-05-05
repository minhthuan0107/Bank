package com.example.bank.controller.admin.notification;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.notification.CreateAnnouncementRequest;
import com.example.bank.dto.request.notification.UpdateAnnouncementRequest;
import com.example.bank.dto.response.notification.AnnouncementPageResponse;
import com.example.bank.dto.response.notification.AnnouncementResponse;
import com.example.bank.service.notification.admin.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/announcements")
public class AnnouncementAdminController {

    private final AnnouncementService announcementService;
    private final LocalizationUtils i18n;

    /**
     * Admin tạo thông báo chung
     */
    @PostMapping("/created")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        AnnouncementResponse response =
                announcementService.createAnnouncement(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        i18n.getLocalizedMessage(MessageKeys.ANNOUNCEMENT_CREATED),
                        response
                ));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAnnouncementRequest request
    ) {
        AnnouncementResponse response =
                announcementService.updateAnnouncement(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ANNOUNCEMENT_UPDATED),
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable Long id
    ) {
        announcementService.deleteAnnouncement(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ANNOUNCEMENT_DELETED),
                        null
                )
        );
    }



}