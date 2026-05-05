package com.example.bank.dto.request.notification;

import com.example.bank.common.constants.MessageKeys;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAnnouncementRequest {

    @NotBlank(message = "{" + MessageKeys.VALIDATION_ANNOUNCEMENT_TITLE_REQUIRED + "}")
    @Size(max = 255, message = "{" + MessageKeys.VALIDATION_ANNOUNCEMENT_TITLE_MAX_LENGTH + "}")
    private String title;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_ANNOUNCEMENT_CONTENT_REQUIRED + "}")
    @Size(max = 5000, message = "{" + MessageKeys.VALIDATION_ANNOUNCEMENT_CONTENT_MAX_LENGTH + "}")
    private String content;

    @JsonProperty("link_url")
    @Size(max = 500, message = "{" + MessageKeys.VALIDATION_ANNOUNCEMENT_LINK_MAX_LENGTH + "}")
    private String linkUrl;
}
