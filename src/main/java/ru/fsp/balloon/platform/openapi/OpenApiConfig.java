package ru.fsp.balloon.platform.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Описание API для Swagger UI.
 *
 * <p>Swagger — основной инструмент проверки для эксперта: все обязательные
 * сценарии проходятся через него, без фронта и без обращения к команде.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI balloonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Воздушный Шар — API")
                        .version("0.1.0")
                        .description("""
                                Бонусная crash-игра. Прототип для онлайн-этапа.

                                С ЧЕГО НАЧАТЬ ПРОВЕРКУ

                                1. GET /api/config/public — что сейчас настроено: варианты
                                   ставок, темы, количество уровней, начисление очков.
                                2. GET /api/users/demo — демонстрационный пользователь
                                   с ненулевым балансом. Пополнить: POST /api/users/demo/topup
                                3. Пройти игровой цикл: создать раунд, забрать выигрыш или
                                   дождаться краха, посмотреть результат.
                                4. GET /api/history — завершённые раунды.

                                СЦЕНАРИЙ 5, УПРАВЛЕНИЕ ПАРАМЕТРАМИ

                                Откройте config/game-config.yml, поменяйте points.perLevel,
                                сохраните. Начните новый раунд — начисление очков изменилось.
                                Перезапускать сервис и править код не нужно.
                                Убедиться, что новая версия применилась: GET /api/config/status
                                Применить немедленно: POST /api/admin/config/reload

                                ОБОСНОВАНИЕ МОДЕЛИ

                                GET /api/config/simulate?rounds=10000&theme=green — распределение
                                точки краха на действующих параметрах. Поменяйте math.alpha
                                и сравните: модель объясняется расчётом, а не одним раундом.

                                ЖУРНАЛ СОБЫТИЙ

                                GET /api/events — что происходило на сервере и в каком порядке.
                                """))
                .servers(List.of(new Server().url("/").description("Текущий сервер")));
    }
}
