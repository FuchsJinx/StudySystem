package ru.unirate.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.infrastructure.config.UnirateProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAI-compatible chat client. Without a configured key the application uses a
 * deterministic demo fallback (provider=demo); a configured but failing endpoint
 * results in 503 AI_UNAVAILABLE.
 */
@Service
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final UnirateProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public LlmClient(UnirateProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public boolean isConfigured() {
        return properties.getLlm().getApiKey() != null
                && !properties.getLlm().getApiKey().isBlank()
                && properties.getLlm().getBaseUrl() != null
                && !properties.getLlm().getBaseUrl().isBlank();
    }

    public String provider() {
        return isConfigured() ? "openai" : "demo";
    }

    public Optional<String> generate(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> request = Map.of(
                    "model", properties.getLlm().getModel(),
                    "temperature", 0.4,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );
            String raw = restClient.post()
                    .uri(chatEndpoint())
                    .header("Authorization", "Bearer " + properties.getLlm().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Empty LLM response");
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new IllegalStateException("Unexpected LLM response shape");
            }
            return Optional.of(content.asText().trim());
        } catch (RuntimeException ex) {
            log.warn("LLM request failed: {}", ex.getMessage());
            throw ApiException.unavailable(ErrorCodes.AI_UNAVAILABLE,
                    "Сервис ИИ временно недоступен. Попробуйте ещё раз позже.");
        } catch (Exception ex) {
            log.warn("LLM response parsing failed: {}", ex.getMessage());
            throw ApiException.unavailable(ErrorCodes.AI_UNAVAILABLE,
                    "Сервис ИИ временно недоступен. Попробуйте ещё раз позже.");
        }
    }

    private String chatEndpoint() {
        String base = properties.getLlm().getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        if (base.endsWith("/v1")) {
            return base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }
}
