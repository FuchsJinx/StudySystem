package ru.unirate.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.CommonDtos;
import ru.unirate.api.dto.FeedDtos;
import ru.unirate.infrastructure.persistence.entity.FeedEvent;
import ru.unirate.infrastructure.persistence.repo.FeedEventRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class PersonalFeedService {

    private final FeedEventRepository repository;

    public PersonalFeedService(FeedEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CommonDtos.PageResponse<FeedDtos.PersonalFeedItemDto> page(Long userId, Long cursor, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 50));
        List<FeedEvent> rows = cursor == null
                ? repository.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, pageSize + 1))
                : repository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, PageRequest.of(0, pageSize + 1));
        boolean hasMore = rows.size() > pageSize;
        List<FeedEvent> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<FeedDtos.PersonalFeedItemDto> items = new ArrayList<>(page.size());
        for (FeedEvent event : page) {
            items.add(new FeedDtos.PersonalFeedItemDto(
                    event.getId(), event.getEventType(), event.getMessage(), event.getCreatedAt()));
        }
        Long nextCursor = hasMore && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new CommonDtos.PageResponse<>(items, nextCursor);
    }
}
