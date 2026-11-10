package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.domain.settings.SettingDefinition;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.persistence.entity.AppSetting;
import ru.unirate.infrastructure.persistence.repo.AppSettingRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingsService {

    private final AppSettingRepository repository;

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public int getInt(String key) {
        SettingDefinition definition = SettingsCatalog.find(key)
                .orElseThrow(() -> new IllegalArgumentException("Unknown setting: " + key));
        return repository.findById(key)
                .map(setting -> parseOrDefault(setting.getValue(), definition))
                .orElseGet(() -> Integer.parseInt(definition.defaultValue()));
    }

    private int parseOrDefault(String raw, SettingDefinition definition) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return Integer.parseInt(definition.defaultValue());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, String> allValues() {
        Map<String, String> values = new LinkedHashMap<>();
        SettingsCatalog.ALL.forEach(definition -> values.put(definition.key(), definition.defaultValue()));
        repository.findAll().forEach(setting -> {
            if (values.containsKey(setting.getKey())) {
                values.put(setting.getKey(), setting.getValue());
            }
        });
        return values;
    }

    @Transactional(readOnly = true)
    public List<SettingDefinition> definitions() {
        return SettingsCatalog.ALL;
    }

    @Transactional
    public Map<String, String> update(Map<String, String> changes) {
        if (changes == null || changes.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.SETTING_INVALID, "Нет настроек для сохранения");
        }
        for (Map.Entry<String, String> change : changes.entrySet()) {
            SettingDefinition definition = SettingsCatalog.find(change.getKey())
                    .orElseThrow(() -> ApiException.badRequest(ErrorCodes.SETTING_INVALID,
                            "Неизвестная настройка: " + change.getKey()));
            int value;
            try {
                value = Integer.parseInt(String.valueOf(change.getValue()).trim());
            } catch (NumberFormatException ex) {
                throw ApiException.badRequest(ErrorCodes.SETTING_INVALID,
                        "Настройка «" + definition.title() + "» должна быть целым числом");
            }
            if (value < definition.min() || value > definition.max()) {
                throw ApiException.badRequest(ErrorCodes.SETTING_INVALID,
                        "Настройка «" + definition.title() + "» должна быть от " + definition.min()
                                + " до " + definition.max());
            }
        }
        for (Map.Entry<String, String> change : changes.entrySet()) {
            AppSetting setting = repository.findById(change.getKey())
                    .orElseGet(() -> new AppSetting(change.getKey(), change.getValue()));
            setting.setValue(String.valueOf(change.getValue()).trim());
            setting.setUpdatedAt(Instant.now());
            repository.save(setting);
        }
        return allValues();
    }
}
