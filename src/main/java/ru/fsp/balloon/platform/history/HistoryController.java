package ru.fsp.balloon.platform.history;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** История завершённых игр — обязательный элемент экрана выбора ставки. */
@RestController
@RequestMapping("/api/history")
@Tag(name = "История игр", description = "Завершённые раунды всех игроков прототипа")
public class HistoryController {

    private final HistoryService history;

    public HistoryController(HistoryService history) {
        this.history = history;
    }

    @GetMapping
    @Operation(
            summary = "Завершённые раунды",
            description = """
                    По умолчанию отдаёт раунды всех пользователей прототипа —
                    именно этого требует постановка для экрана выбора ставки.
                    Параметр username сужает выборку до одного игрока.

                    Коэффициент краха показывается и у выигранных раундов: игрок
                    видит, сколько мог бы забрать.
                    """
    )
    public Map<String, Object> recent(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var result = history.recent(username, page, size);

        List<Map<String, Object>> items = result.getContent().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("roundId", e.getRoundId());
                    m.put("username", e.getUsername());
                    m.put("theme", e.getTheme());
                    m.put("bet", e.getBet());
                    m.put("cashoutMultiplier", e.getCashoutMultiplier());
                    m.put("crashMultiplier", e.getCrashMultiplier());
                    m.put("payout", e.getPayout());
                    m.put("pointsEarned", e.getPointsEarned());
                    m.put("reward", e.getReward());
                    m.put("boosterHit", e.isBoosterHit());
                    m.put("won", e.isWon());
                    m.put("finishedAt", e.getFinishedAt());
                    return m;
                })
                .toList();

        return Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "size", size,
                "items", items);
    }
}
