package ru.unirate.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.unirate.infrastructure.config.UnirateProperties;
import ru.unirate.infrastructure.mail.MailService;
import ru.unirate.infrastructure.persistence.entity.NotificationSettings;
import ru.unirate.infrastructure.persistence.entity.User;
import ru.unirate.infrastructure.persistence.repo.NotificationSettingsRepository;
import ru.unirate.infrastructure.telegram.TelegramClient;

/** Delivers notifications to external channels after the in-app record is created. */
@Service
public class ExternalNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(ExternalNotificationSender.class);

    private final NotificationSettingsRepository settingsRepository;
    private final MailService mailService;
    private final TelegramClient telegramClient;
    private final UnirateProperties properties;

    public ExternalNotificationSender(NotificationSettingsRepository settingsRepository,
                                      MailService mailService,
                                      TelegramClient telegramClient,
                                      UnirateProperties properties) {
        this.settingsRepository = settingsRepository;
        this.mailService = mailService;
        this.telegramClient = telegramClient;
        this.properties = properties;
    }

    @Async("notificationExecutor")
    public void send(User user, String title, String body) {
        if (!properties.getNotifications().isExternalEnabled()) {
            return;
        }
        try {
            NotificationSettings settings = settingsRepository.findByUserId(user.getId()).orElse(null);
            boolean emailEnabled = settings == null || settings.isEmailEnabled();
            if (emailEnabled && properties.getMail().isEnabled()) {
                mailService.sendNotificationEmail(user, title, body);
            }
            if (settings != null && settings.isTelegramEnabled()
                    && settings.getTelegramChatId() != null
                    && !settings.getTelegramChatId().isBlank()) {
                telegramClient.sendMessage(settings.getTelegramChatId(), title + "\n\n" + body);
            }
        } catch (RuntimeException ex) {
            log.warn("Не удалось отправить внешнее уведомление пользователю {}: {}",
                    user.getId(), ex.getMessage());
        }
    }
}
