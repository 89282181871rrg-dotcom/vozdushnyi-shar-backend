package ru.fsp.balloon.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Единый источник игровой конфигурации.
 *
 * <p>Закрывает обязательный сценарий 5: эксперт меняет параметр в файле,
 * начинает новый раунд и видит новое поведение — без перезапуска сервиса
 * и без правки исходного кода.
 *
 * <h2>Как устроена горячая перезагрузка</h2>
 * Перед выдачей конфигурации сверяется время изменения файла. Если файл
 * тронули — он перечитывается, проверяется и только потом применяется.
 * Отдельного действия от эксперта не требуется, но ручная перезагрузка
 * тоже есть: {@code POST /api/admin/config/reload}.
 *
 * <h2>Почему так, а не через @RefreshScope</h2>
 * Spring Cloud тянул бы лишнюю зависимость ради одной функции. Здесь
 * достаточно проверки mtime: дёшево, предсказуемо и объяснимо на защите.
 *
 * <h2>Почему идущий раунд не перечитывает конфиг</h2>
 * Движок раунда берёт снимок конфигурации один раз, при создании раунда,
 * и доигрывает по нему. Иначе правка файла в середине полёта поменяла бы
 * правила на ходу, и результат стало бы невозможно воспроизвести.
 */
@Service
public class GameConfigService {

    private static final Logger log = LoggerFactory.getLogger(GameConfigService.class);

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    private final GameConfigValidator validator;

    private final Path configPath;
    private final boolean autoReload;

    private final AtomicReference<GameConfig> current = new AtomicReference<>();
    private volatile long lastModified = -1;
    private volatile Instant loadedAt;
    private volatile List<String> lastErrors = List.of();

    public GameConfigService(
            GameConfigValidator validator,
            @Value("${balloon.config.path:config/game-config.yml}") String path,
            @Value("${balloon.config.auto-reload:true}") boolean autoReload) {
        this.validator = validator;
        this.configPath = Path.of(path);
        this.autoReload = autoReload;
    }

    @PostConstruct
    void init() {
        ReloadResult result = reload();
        if (!result.applied()) {
            throw new IllegalStateException(
                    "Стартовая конфигурация неверна, запускаться не с чем:\n  - "
                            + String.join("\n  - ", result.errors()));
        }
        log.info("Конфигурация игры загружена из {}", configPath.toAbsolutePath());
    }

    /**
     * Действующая конфигурация. Если файл изменился — перечитывает его.
     *
     * <p>Это точка, через которую конфигурацию получают все остальные модули.
     * Движок раунда вызывает её один раз при создании раунда и дальше держит
     * полученный снимок.
     */
    public GameConfig get() {
        if (autoReload && fileChanged()) {
            reload();
        }
        return current.get();
    }

    /** Перезагрузка по запросу — {@code POST /api/admin/config/reload}. */
    public ReloadResult reload() {
        GameConfig parsed;
        try {
            parsed = read();
        } catch (IOException e) {
            String message = "Не удалось прочитать " + configPath.toAbsolutePath() + ": " + e.getMessage();
            log.error(message);
            lastErrors = List.of(message);
            return new ReloadResult(false, lastErrors, loadedAt);
        }

        List<String> errors = validator.validate(parsed);
        if (!errors.isEmpty()) {
            // Ключевое поведение: сломанный файл НЕ применяется.
            // Сервис продолжает работать на прошлой рабочей конфигурации.
            log.warn("Новая конфигурация отклонена, {} ошибок. Работаем на прежней версии:", errors.size());
            errors.forEach(e -> log.warn("  - {}", e));
            lastErrors = errors;
            // отметим mtime, чтобы не перечитывать сломанный файл на каждом запросе
            touchMtime();
            return new ReloadResult(false, errors, loadedAt);
        }

        current.set(parsed);
        loadedAt = Instant.now();
        lastErrors = List.of();
        touchMtime();
        log.info("Конфигурация применена");
        return new ReloadResult(true, List.of(), loadedAt);
    }

    /** Когда конфигурация была успешно применена в последний раз. */
    public Instant loadedAt() {
        return loadedAt;
    }

    /** Ошибки последней неудачной попытки загрузки. Пустой список — всё в порядке. */
    public List<String> lastErrors() {
        return lastErrors;
    }

    public Path configPath() {
        return configPath.toAbsolutePath();
    }

    // ------------------------------------------------------------------------

    private GameConfig read() throws IOException {
        if (Files.exists(configPath)) {
            return yaml.readValue(Files.readString(configPath), GameConfig.class);
        }
        // Запасной вариант: конфигурация из ресурсов jar. Нужен, чтобы сервис
        // поднялся в контейнере, куда файл не примонтировали.
        log.warn("Файл {} не найден, беру конфигурацию по умолчанию из ресурсов",
                configPath.toAbsolutePath());
        try (InputStream in = new ClassPathResource("default-game-config.yml").getInputStream()) {
            return yaml.readValue(in, GameConfig.class);
        }
    }

    private boolean fileChanged() {
        try {
            if (!Files.exists(configPath)) {
                return false;
            }
            return Files.getLastModifiedTime(configPath).toMillis() != lastModified;
        } catch (IOException e) {
            return false;
        }
    }

    private void touchMtime() {
        try {
            if (Files.exists(configPath)) {
                lastModified = Files.getLastModifiedTime(configPath).toMillis();
            }
        } catch (IOException ignored) {
            // не критично: в худшем случае перечитаем файл лишний раз
        }
    }

    /**
     * Результат попытки применить конфигурацию.
     *
     * @param applied  применена ли новая версия
     * @param errors   что именно не так, если не применена
     * @param loadedAt когда действующая версия была применена
     */
    public record ReloadResult(boolean applied, List<String> errors, Instant loadedAt) {}
}
