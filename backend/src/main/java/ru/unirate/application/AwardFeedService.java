package ru.unirate.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.CommonDtos;
import ru.unirate.api.dto.FeedDtos;
import ru.unirate.infrastructure.persistence.entity.AwardFeedItem;
import ru.unirate.infrastructure.persistence.repo.AwardFeedItemRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class AwardFeedService {

    private final AwardFeedItemRepository repository;

    public AwardFeedService(AwardFeedItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CommonDtos.PageResponse<FeedDtos.AwardFeedItemDto> page(Long cursor, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 50));
        List<AwardFeedItem> rows = cursor == null
                ? repository.findAllByOrderByIdDesc(PageRequest.of(0, pageSize + 1))
                : repository.findByIdLessThanOrderByIdDesc(cursor, PageRequest.of(0, pageSize + 1));
        boolean hasMore = rows.size() > pageSize;
        List<AwardFeedItem> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<FeedDtos.AwardFeedItemDto> items = new ArrayList<>(page.size());
        for (AwardFeedItem item : page) {
            items.add(new FeedDtos.AwardFeedItemDto(
                    item.getId(),
                    item.getStudentProfile().getUser().getId(),
                    item.getStudentProfile().getUser().getFullName(),
                    item.getStudentProfile().getGroup().getName(),
                    item.getCurrency(),
                    item.getAmount(),
                    item.getActivityTitle(),
                    item.getTeacherName(),
                    item.getCreatedAt()
            ));
        }
        Long nextCursor = hasMore && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new CommonDtos.PageResponse<>(items, nextCursor);
    }
}
