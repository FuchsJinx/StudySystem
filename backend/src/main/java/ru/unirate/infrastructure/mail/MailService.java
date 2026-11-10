package ru.unirate.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.unirate.infrastructure.config.UnirateProperties;
import ru.unirate.infrastructure.persistence.entity.User;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final UnirateProperties properties;

    public MailService(JavaMailSender mailSender, UnirateProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendVerificationEmail(User user, String token) {
        String link = properties.getAppUrl() + "/verify-email?token=" + token;
        String body = """
                Здравствуйте, %s!

                Подтвердите адрес электронной почты в UniRate, перейдя по ссылке:
                %s

                Ссылка действует 24 часа. Если вы не регистрировались, проигнорируйте это письмо.
                """.formatted(user.getFullName(), link);
        send(user.getEmail(), "UniRate — подтверждение email", body);
    }

    public void sendNotificationEmail(User user, String subject, String body) {
        send(user.getEmail(), subject, body + "\n\n— UniRate");
    }

    private void send(String to, String subject, String body) {
        if (!properties.getMail().isEnabled()) {
            log.debug("Mail disabled, skipping message to {}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getMail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (RuntimeException ex) {
            log.warn("Не удалось отправить письмо на {}: {}", to, ex.getMessage());
        }
    }
}
