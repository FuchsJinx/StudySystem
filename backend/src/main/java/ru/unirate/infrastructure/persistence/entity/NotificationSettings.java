package ru.unirate.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
public class NotificationSettings {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "telegram_enabled", nullable = false)
    private boolean telegramEnabled = false;

    @Column(name = "telegram_chat_id", length = 64)
    private String telegramChatId;

    public NotificationSettings(Long userId) {
        this.userId = userId;
    }
}
