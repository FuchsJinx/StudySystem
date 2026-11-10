package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.infrastructure.persistence.entity.FeedEvent;
import ru.unirate.infrastructure.persistence.entity.Notification;
import ru.unirate.infrastructure.persistence.entity.User;
import ru.unirate.infrastructure.persistence.repo.FeedEventRepository;
import ru.unirate.infrastructure.persistence.repo.NotificationRepository;

/** Writes personal feed events and in-app notifications in the current transaction. */
@Service
public class EventService {

    private final FeedEventRepository feedEventRepository;
    private final NotificationRepository notificationRepository;

    public EventService(FeedEventRepository feedEventRepository, NotificationRepository notificationRepository) {
        this.feedEventRepository = feedEventRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void feed(User user, FeedEventType type, String message) {
        FeedEvent event = new FeedEvent();
        event.setUser(user);
        event.setEventType(type.name());
        event.setMessage(message);
        feedEventRepository.save(event);
    }

    @Transactional
    public Notification notify(User user, String title, String body, FeedEventType kind, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setKind(kind == null ? null : kind.name());
        notification.setLink(link);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void feedAndNotify(User user, FeedEventType type, String message,
                              String title, String body, String link) {
        feed(user, type, message);
        notify(user, title, body, type, link);
    }
}
