package ru.unirate.domain.ai;

import org.junit.jupiter.api.Test;
import ru.unirate.infrastructure.config.UnirateProperties;
import ru.unirate.infrastructure.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplatesTest {

    @Test
    void ratingPromptContainsAggregatesOnly() {
        String prompt = PromptTemplates.ratingTips(820, 150, 75, 2, 4, "01.12.2026 18:00");
        assertThat(prompt).contains("820").contains("150").contains("75");
        assertThat(prompt).doesNotContain("@").doesNotContain("student@");
        assertThat(prompt).contains("Просроченных заданий: 2");
    }

    @Test
    void llmWithoutKeyUsesDemoMode() {
        UnirateProperties properties = new UnirateProperties();
        properties.getLlm().setApiKey("");
        properties.getLlm().setBaseUrl("");
        LlmClient client = new LlmClient(properties, new ObjectMapper());
        assertThat(client.isConfigured()).isFalse();
        assertThat(client.provider()).isEqualTo("demo");
        assertThat(client.generate("system", "user")).isEmpty();
    }

    @Test
    void draftPromptContainsThesesAndTitle() {
        String prompt = PromptTemplates.draftCheckpoint("Лаба 3", "Реализовать алгоритм Дейкстры");
        assertThat(prompt).contains("Лаба 3").contains("Дейкстры");
    }
}
