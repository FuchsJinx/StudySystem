package ru.unirate.infrastructure.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.unirate.infrastructure.config.UnirateProperties;

import java.util.Map;

/**
 * Telegram Bot API transport. Without a configured token it works as a documented
 * mock transport: the message is logged and no external request is made.
 */
@Service
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final UnirateProperties properties;
    private final RestClient restClient;

    public TelegramClient(UnirateProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public void sendMessage(String chatId, String text) {
        if (chatId == null || chatId.isBlank()) {
            return;
        }
        String token = properties.getTelegram().getBotToken();
        if (token == null || token.isBlank()) {
            log.info("Telegram mock: chatId={}, text={}", chatId, text);
            return;
        }
        try {
            restClient.post()
                    .uri(properties.getTelegram().getApiBase() + "/bot{token}/sendMessage", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", text))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram message sent to chat {}", chatId);
        } catch (RuntimeException ex) {
            log.warn("Не удалось отправить Telegram-сообщение в чат {}: {}", chatId, ex.getMessage());
        }
    }
}
