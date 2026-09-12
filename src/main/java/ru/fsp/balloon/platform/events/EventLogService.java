package ru.fsp.balloon.platform.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Журнал игровых событий.
 *
 * <p>Пишется в отдельной транзакции: запись в журнал никогда не должна
 * откатить игровую операцию и никогда не должна её уронить. Если журнал
 * почему-то не записался — игра продолжается, в лог уходит предупреждение.
 */
@Service
public class EventLogService {

    private static final Logger log = LoggerFactory.getLogger(EventLogService.class);

    private final GameEventRepository repository;
    private final ObjectMapper json = new ObjectMapper();

    public EventLogService(GameEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String type, Map<String, ?> details) {
        try {
            repository.save(new GameEvent(type, json.writeValueAsString(details)));
        } catch (JsonProcessingException e) {
            repository.save(new GameEvent(type, String.valueOf(details)));
        } catch (Exception e) {
            log.warn("Не удалось записать событие {} в журнал: {}", type, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<GameEvent> recent(String type, int page, int size) {
        PageRequest request = PageRequest.of(page, java.lang.Math.min(size, 500));
        return type == null || type.isBlank()
                ? repository.findAllByOrderByCreatedAtDesc(request)
                : repository.findByTypeOrderByCreatedAtDesc(type, request);
    }
}
