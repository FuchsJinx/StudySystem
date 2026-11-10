package ru.unirate.domain.settings;

public record SettingDefinition(
        String key,
        String title,
        String description,
        String defaultValue,
        int min,
        int max,
        String unit
) {
    public boolean isInteger() {
        return true;
    }
}
