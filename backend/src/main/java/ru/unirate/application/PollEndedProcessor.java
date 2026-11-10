package ru.unirate.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.infrastructure.persistence.entity.Poll;
import ru.unirate.infrastructure.persistence.repo.PollRepository;

import java.time.Instant;
import java.util.List;

/** Marks finished polls once and publishes POLL_ENDED to the teacher (spec 11 / 12). */
@Service
public class PollEndedProcessor {

    private static final Logger log = LoggerFactory.getLogger(PollEndedProcessor.class);

    private final PollRepository pollRepository;
    private final EventService eventService;

    public PollEndedProcessor(PollRepository pollRepository, EventService eventService) {
        this.pollRepository = pollRepository;
        this.eventService = eventService;
    }

    @Transactional
    public int processEnded() {
        List<Poll> polls = pollRepository.findByEndsAtBeforeAndEndedNotifiedFalse(Instant.now());
        for (Poll poll : polls) {
            poll.setEndedNotified(true);
            pollRepository.save(poll);
            String message = "Опрос «" + poll.getTitle() + "» завершён";
            eventService.feedAndNotify(poll.getCreatedBy().getUser(), FeedEventType.POLL_ENDED, message,
                    "Опрос завершён", message + ". Результаты доступны в разделе опросов.",
                    "/polls/" + poll.getId());
            log.info("Poll ended: id={}, title={}", poll.getId(), poll.getTitle());
        }
        return polls.size();
    }
}
