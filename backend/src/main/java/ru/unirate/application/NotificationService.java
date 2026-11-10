package ru.unirate.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.CommonDtos;
import ru.unirate.api.dto.NotificationDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.infrastructure.persistence.entity.Notification;
import ru.unirate.infrastructure.persistence.repo.NotificationRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CommonDtos.PageResponse<NotificationDtos.NotificationDto> page(Long userId, Long cursor, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 50));
        List<Notification> rows = cursor == null
                ? repository.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, pageSize + 1))
                : repository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, PageRequest.of(0, pageSize + 1));
        boolean hasMore = rows.size() > pageSize;
        List<Notification> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<NotificationDtos.NotificationDto> items = new ArrayList<>(page.size());
        for (Notification notification : page) {
            items.add(toDto(notification));
        }
        Long nextCursor = hasMore && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new CommonDtos.PageResponse<>(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationDtos.NotificationDto markRead(Long userId, Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Уведомление не найдено"));
        if (!notification.getUser().getId().equals(userId)) {
            throw ApiException.notFound(ErrorCodes.NOT_FOUND, "Уведомление не найдено");
        }
        if (!notification.isRead()) {
            notification.setReadAt(Instant.now());
            repository.save(notification);
        }
        return toDto(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, Instant.now());
    }

    private NotificationDtos.NotificationDto toDto(Notification notification) {
        return new NotificationDtos.NotificationDto(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getKind(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
