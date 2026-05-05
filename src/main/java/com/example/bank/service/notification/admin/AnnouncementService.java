package com.example.bank.service.notification.admin;

import com.example.bank.dto.request.notification.CreateAnnouncementRequest;
import com.example.bank.dto.request.notification.UpdateAnnouncementRequest;
import com.example.bank.dto.response.notification.AnnouncementPageResponse;
import com.example.bank.dto.response.notification.AnnouncementResponse;

public interface AnnouncementService {
    AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request);

    AnnouncementResponse updateAnnouncement(
            Long id,
            UpdateAnnouncementRequest request
    );

    void deleteAnnouncement(Long id);

    AnnouncementPageResponse getActiveAnnouncements(int page);
}
