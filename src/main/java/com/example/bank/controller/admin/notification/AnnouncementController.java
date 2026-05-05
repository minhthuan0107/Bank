package com.example.bank.controller.admin.notification;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.notification.AnnouncementPageResponse;
import com.example.bank.service.notification.admin.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final LocalizationUtils i18n;

    /**
     * Public API: guest/user/admin đều xem được danh sách thông báo đang active
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<AnnouncementPageResponse>> getAnnouncements(
            @RequestParam(defaultValue = "0") int page
    ) {
        AnnouncementPageResponse response =
                announcementService.getActiveAnnouncements(page);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ANNOUNCEMENT_LIST_SUCCESS),
                        response
                )
        );
    }

}
