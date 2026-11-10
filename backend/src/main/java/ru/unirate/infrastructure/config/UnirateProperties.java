package ru.unirate.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "unirate")
public class UnirateProperties {

    private final Jwt jwt = new Jwt();
    private final Uploads uploads = new Uploads();
    private final Worker worker = new Worker();
    private final Llm llm = new Llm();
    private final Telegram telegram = new Telegram();
    private final Mail mail = new Mail();
    private final Notifications notifications = new Notifications();
    private final DemoSeed demoSeed = new DemoSeed();

    /** Public base URL used in e-mail links. */
    private String appUrl = "http://localhost";

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "change-me-in-production-use-long-random-string";
        private long ttlSeconds = 86400;
    }

    @Getter
    @Setter
    public static class Uploads {
        private String dir = "/data/uploads";
        private long maxBytes = 10 * 1024 * 1024L;
    }

    @Getter
    @Setter
    public static class Worker {
        private boolean enabled = false;
        private long tickMs = 1000;
        private long reminderDelayMs = 300_000;
        private long pollDelayMs = 60_000;
    }

    @Getter
    @Setter
    public static class Llm {
        private String apiKey = "";
        private String baseUrl = "";
        private String model = "gpt-4o-mini";
        private int timeoutSeconds = 30;
    }

    @Getter
    @Setter
    public static class Telegram {
        private String botToken = "";
        private String apiBase = "https://api.telegram.org";
    }

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = true;
        private String from = "noreply@unirate.local";
    }

    @Getter
    @Setter
    public static class Notifications {
        /** External e-mail/telegram delivery switch (off in tests / offline demo). */
        private boolean externalEnabled = true;
    }

    @Getter
    @Setter
    public static class DemoSeed {
        private boolean enabled = true;
    }
}
