package com.example.bank.entity.notification;

import com.example.bank.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tiêu đề thông báo
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Nội dung thông báo
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Link đính kèm nếu có
     */
    @Column(name = "link_url", length = 500)
    private String linkUrl;


    /**
     * true = hiển thị cho user
     * false = ẩn
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    public void updateContent(
            String title,
            String content,
            String linkUrl
    ) {
        this.title = title;
        this.content = content;
        this.linkUrl = linkUrl;
    }

    public void deactivate() {
        this.isActive = false;
    }



}
