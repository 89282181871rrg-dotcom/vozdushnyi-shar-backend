package ru.fsp.balloon.platform.events;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Чтение журнала событий — для независимой проверки серверных операций. */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Журнал событий", description = "Что происходило на сервере и в каком порядке")
public class EventsController {

    private final EventLogService events;

    public EventsController(EventLogService events) {
        this.events = events;
    }

    @GetMapping
    @Operation(
            summary = "Последние события",
            description = """
                    Типы: round_started, booster_hit, cashout, crash, config_reloaded, balance_topup.

                    Позволяет сопоставить поведение сервиса с описанной моделью:
                    видно, что точка краха и позиция бустера определены до старта раунда,
                    а не подогнаны по ходу.
                    """
    )
    public Map<String, Object> recent(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        var result = events.recent(type, page, size);
        List<Map<String, Object>> items = result.getContent().stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "type", e.getType(),
                        "details", e.getDetails(),
                        "createdAt", e.getCreatedAt()))
                .toList();

        return Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "size", size,
                "items", items);
    }
}
