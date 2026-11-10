package ru.unirate.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.unirate.infrastructure.config.UnirateProperties;
import ru.unirate.infrastructure.mail.MailService;
import ru.unirate.infrastructure.persistence.entity.NotificationSettings;
import ru.unirate.infrastructure.persistence.entity.User;
import ru.unirate.infrastructure.persistence.repo.NotificationSettingsRepository;
import ru.unirate.infrastructure.telegram.TelegramClient;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalNotificationSenderTest {

    private NotificationSettingsRepository settingsRepository;
    private MailService mailService;
    private TelegramClient telegramClient;
    private UnirateProperties properties;
    private ExternalNotificationSender sender;
    private User user;

    @BeforeEach
    void setUp() {
        settingsRepository = Mockito.mock(NotificationSettingsRepository.class);
        mailService = Mockito.mock(MailService.class);
        telegramClient = Mockito.mock(TelegramClient.class);
        properties = new UnirateProperties();
        properties.getMail().setEnabled(true);
        properties.getNotifications().setExternalEnabled(true);
        sender = new ExternalNotificationSender(settingsRepository, mailService, telegramClient, properties);
        user = new User();
        user.setId(4L);
        user.setEmail("student@demo.local");
        user.setFullName("Иван Иванов");
    }

    @Test
    void sendsEmailWhenEnabled() {
        NotificationSettings settings = new NotificationSettings(4L);
        settings.setEmailEnabled(true);
        settings.setTelegramEnabled(false);
        when(settingsRepository.findByUserId(4L)).thenReturn(Optional.of(settings));

        sender.send(user, "Тема", "Текст");

        verify(mailService).sendNotificationEmail(user, "Тема", "Текст");
        verify(telegramClient, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void doesNotSendEmailWhenDisabled() {
        NotificationSettings settings = new NotificationSettings(4L);
        settings.setEmailEnabled(false);
        when(settingsRepository.findByUserId(4L)).thenReturn(Optional.of(settings));

        sender.send(user, "Тема", "Текст");

        verify(mailService, never()).sendNotificationEmail(user, "Тема", "Текст");
    }

    @Test
    void sendsTelegramWhenConfigured() {
        NotificationSettings settings = new NotificationSettings(4L);
        settings.setEmailEnabled(false);
        settings.setTelegramEnabled(true);
        settings.setTelegramChatId("123456");
        when(settingsRepository.findByUserId(4L)).thenReturn(Optional.of(settings));

        sender.send(user, "Тема", "Текст");

        verify(telegramClient).sendMessage(eq("123456"), anyString());
        verify(mailService, never()).sendNotificationEmail(user, "Тема", "Текст");
    }

    @Test
    void respectsGlobalExternalSwitch() {
        properties.getNotifications().setExternalEnabled(false);
        NotificationSettings settings = new NotificationSettings(4L);
        settings.setEmailEnabled(true);
        settings.setTelegramEnabled(true);
        settings.setTelegramChatId("123456");
        when(settingsRepository.findByUserId(4L)).thenReturn(Optional.of(settings));

        sender.send(user, "Тема", "Текст");

        verify(mailService, never()).sendNotificationEmail(user, "Тема", "Текст");
        verify(telegramClient, never()).sendMessage(anyString(), anyString());
    }
}
