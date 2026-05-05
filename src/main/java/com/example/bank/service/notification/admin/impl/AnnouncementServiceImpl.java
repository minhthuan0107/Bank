package com.example.bank.service.notification.admin.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.notification.CreateAnnouncementRequest;
import com.example.bank.dto.request.notification.UpdateAnnouncementRequest;
import com.example.bank.dto.response.notification.AnnouncementPageResponse;
import com.example.bank.dto.response.notification.AnnouncementResponse;
import com.example.bank.entity.notification.Announcement;
import com.example.bank.repository.notification.AnnouncementRepository;
import com.example.bank.service.notification.admin.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final WalletProperties walletProperties;

    @Override
    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .linkUrl(normalize(request.getLinkUrl()))
                .isActive(true)
                .build();

        announcementRepository.save(announcement);
        return AnnouncementResponse.from(announcement);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Override
    @Transactional
    public AnnouncementResponse updateAnnouncement(
            Long id,
            UpdateAnnouncementRequest request
    ) {

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.ANNOUNCEMENT_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (Boolean.FALSE.equals(announcement.getIsActive())) {
            throw new WalletException(
                    MessageKeys.ANNOUNCEMENT_NOT_FOUND,
                    HttpStatus.NOT_FOUND
            );
        }

        announcement.updateContent(
                request.getTitle().trim(),
                request.getContent().trim(),
                normalize(request.getLinkUrl())
        );

        return AnnouncementResponse.from(announcement);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id) {


        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.ANNOUNCEMENT_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (Boolean.FALSE.equals(announcement.getIsActive())) {
            return;
        }

        announcement.deactivate();
    }
    @Override
    @Transactional(readOnly = true)
    public AnnouncementPageResponse getActiveAnnouncements(int page) {
        if (page < 0) {
            page = 0;
        }

        int size = walletProperties.getDefaultPageSize();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Announcement> result =
                announcementRepository.findByIsActiveTrue(pageable);

        List<AnnouncementResponse> items = result.getContent()
                .stream()
                .map(AnnouncementResponse::from)
                .toList();

        return AnnouncementPageResponse.builder()
                .items(items)
                .page(page)
                .size(pageable.getPageSize())
                .totalSize(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

}