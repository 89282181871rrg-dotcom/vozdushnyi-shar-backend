package ru.fsp.balloon.platform.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.fsp.balloon.platform.config.GameConfig;
import ru.fsp.balloon.platform.config.GameConfigService;
import ru.fsp.balloon.platform.user.User;
import ru.fsp.balloon.platform.user.UserRepository;

/**
 * Создаёт демонстрационного пользователя при старте, если его ещё нет.
 *
 * <p>Без этого эксперт не пройдёт ни одного сценария: играть не на что.
 * Имя и стартовый баланс берутся из конфигурации, секция {@code demo}.
 *
 * <p>Повторный запуск ничего не ломает: если пользователь уже есть,
 * баланс не сбрасывается.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final GameConfigService config;

    public DemoDataSeeder(UserRepository users, GameConfigService config) {
        this.users = users;
        this.config = config;
    }

    @Override
    public void run(ApplicationArguments args) {
        GameConfig.Demo demo = config.get().demo();

        users.findByUsername(demo.username()).ifPresentOrElse(
                existing -> log.info("Демо-пользователь «{}» уже есть, баланс {} бонусов",
                        existing.getUsername(), existing.getBonusBalance()),
                () -> {
                    User created = users.save(new User(demo.username(), demo.startingBalance()));
                    log.info("Создан демо-пользователь «{}» с балансом {} бонусов",
                            created.getUsername(), created.getBonusBalance());
                });

        log.info("Проверка решения: Swagger на /swagger-ui.html, конфигурация на /api/config/public");
    }
}
