package ru.fsp.balloon.platform.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.balloon.platform.events.EventLogService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Конфигурация наружу: что сейчас в силе, как перезагрузить, как обосновать модель.
 *
 * <p>Закрывает обязательный сценарий 5 и критерий «Техническая проверяемость backend».
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Конфигурация", description = "Параметры игры, горячая перезагрузка, проверка модели")
public class ConfigController {

    private final GameConfigService configService;
    private final EventLogService events;

    public ConfigController(GameConfigService configService, EventLogService events) {
        this.configService = configService;
        this.events = events;
    }

    @GetMapping("/config/public")
    @Operation(
            summary = "Действующая конфигурация для клиента",
            description = """
                    Всё, что фронту нужно для отрисовки: варианты ставок, темы с количеством
                    уровней и границами коэффициентов, правила начисления очков и награды.

                    Секретов здесь нет: точка краха и позиция бустера вычисляются на сервере
                    для каждого раунда отдельно и клиенту не отдаются ни при каких условиях.
                    """
    )
    public Map<String, Object> publicConfig() {
        GameConfig c = configService.get();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("game", c.game());
        out.put("bets", c.bets());
        out.put("themes", c.themes());
        out.put("points", c.points());
        out.put("reward", c.reward());
        out.put("round", c.round());
        out.put("upsellEnabled", c.upsell() != null && c.upsell().enabled());
        out.put("configLoadedAt", configService.loadedAt());
        return out;
    }

    @PostMapping("/admin/config/reload")
    @Operation(
            summary = "Перезагрузить конфигурацию из файла",
            description = """
                    Обычно не нужен: сервис сам замечает изменение файла и подхватывает его
                    перед следующим раундом. Эндпоинт существует, чтобы применить правку
                    немедленно и сразу увидеть ошибки, если в файле что-то не так.

                    При ошибке возвращается 422 со списком проблем, а в силе остаётся
                    прошлая рабочая конфигурация — игра не ломается.
                    """
    )
    public ResponseEntity<Map<String, Object>> reload() {
        GameConfigService.ReloadResult result = configService.reload();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applied", result.applied());
        body.put("configPath", configService.configPath().toString());
        body.put("loadedAt", result.loadedAt());

        if (result.applied()) {
            events.log("config_reloaded", Map.of("path", configService.configPath().toString()));
            body.put("message", "Конфигурация применена. Следующий раунд играет по новым правилам.");
            return ResponseEntity.ok(body);
        }

        body.put("errors", result.errors());
        body.put("message", "Конфигурация отклонена. Работает прошлая рабочая версия.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @GetMapping("/config/status")
    @Operation(
            summary = "Состояние конфигурации",
            description = "Откуда читается файл, когда применена действующая версия, "
                    + "какие ошибки были при последней неудачной попытке."
    )
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configPath", configService.configPath().toString());
        out.put("loadedAt", configService.loadedAt());
        out.put("healthy", configService.lastErrors().isEmpty());
        out.put("lastErrors", configService.lastErrors());
        return out;
    }

    @GetMapping("/config/simulate")
    @Operation(
            summary = "Прогон модели: распределение точки краха",
            description = """
                    Обоснование математической модели расчётами, а не исходом одного
                    случайного раунда. Прогоняет заданное число раундов на действующей
                    конфигурации и показывает распределение.

                    Смените math.alpha в файле конфигурации и вызовите эндпоинт ещё раз —
                    увидите, как поменялась доля длинных полётов и средний коэффициент.

                    Параметр seed делает прогон воспроизводимым: один и тот же seed
                    всегда даёт одинаковый результат.
                    """
    )
    public Map<String, Object> simulate(
            @RequestParam(defaultValue = "10000") int rounds,
            @RequestParam(defaultValue = "green") String theme,
            @RequestParam(required = false) Long seed) {

        if (rounds < 1 || rounds > 1_000_000) {
            throw new IllegalArgumentException("rounds: допустимо от 1 до 1000000");
        }

        GameConfig c = configService.get();
        GameConfig.Theme t = c.theme(theme);
        GameConfig.Math m = c.math();

        SplittableRandom rnd = seed != null ? new SplittableRandom(seed) : new SplittableRandom();

        double sum = 0;
        double max = 0;
        int[] byLevel = new int[t.levels() + 1];
        int belowFirstLevel = 0;
        double firstLevelBound = t.multiplierAtLevel(1);

        for (int i = 0; i < rounds; i++) {
            double crash = crashPoint(m, rnd.nextDouble());
            sum += crash;
            max = java.lang.Math.max(max, crash);

            if (crash < firstLevelBound) {
                belowFirstLevel++;
            }
            byLevel[t.levelReached(crash)]++;
        }

        Map<String, Object> levels = new LinkedHashMap<>();
        for (int lvl = 0; lvl <= t.levels(); lvl++) {
            levels.put("уровень_" + lvl, percent(byLevel[lvl], rounds));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("theme", theme);
        out.put("rounds", rounds);
        out.put("seed", seed);
        out.put("параметры", Map.of(
                "alpha", m.alpha(),
                "minCrashMultiplier", m.minCrashMultiplier(),
                "maxMultiplier", m.maxMultiplier(),
                "houseEdge", m.houseEdge()));
        out.put("среднийКоэффициент", round2(sum / rounds));
        out.put("максимальныйКоэффициент", round2(max));
        out.put("крахДоПервогоУровня", percent(belowFirstLevel, rounds)
                + " — в этих раундах забрать выигрыш было невозможно");
        out.put("доляРаундовПоДостигнутымУровням", levels);
        return out;
    }

    /**
     * Точка краха. Та же формула, что в движке раунда: crash = min / (1-u)^alpha,
     * с поправкой на долю заведения и обрезкой по потолку.
     */
    private double crashPoint(GameConfig.Math m, double u) {
        double adjusted = u * (1.0 - m.houseEdge());
        double crash = m.minCrashMultiplier() / java.lang.Math.pow(1.0 - adjusted, m.alpha());
        return java.lang.Math.min(crash, m.maxMultiplier());
    }

    private String percent(int count, int total) {
        return round2(100.0 * count / total) + "%";
    }

    private double round2(double v) {
        return java.lang.Math.round(v * 100.0) / 100.0;
    }

    /** Неверные параметры запроса — понятный 400, а не стектрейс. */
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "bad_request",
                "message", e.getMessage(),
                "errors", List.of(e.getMessage())));
    }
}
