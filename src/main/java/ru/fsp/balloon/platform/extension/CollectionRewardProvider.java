package ru.fsp.balloon.platform.extension;

import org.springframework.stereotype.Component;
import ru.fsp.balloon.platform.config.GameConfig;
import ru.fsp.balloon.platform.config.GameConfigService;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Награда по умолчанию — фрагмент коллекции.
 *
 * <p>Механика: за раунд можно получить фрагмент, собрал весь набор — получил
 * бонусные баллы. Так награда встроена в прогресс и мотивирует вернуться,
 * а не висит декоративной иконкой на экране результата. Шансы выпадения
 * и размер набора задаются в конфигурации, секция {@code reward}.
 *
 * <p>Когда появится интеграция с лотерейной системой, она встанет второй
 * реализацией {@link RewardProvider} — игровое ядро при этом не меняется.
 */
@Component
public class CollectionRewardProvider implements RewardProvider {

    private final GameConfigService config;

    public CollectionRewardProvider(GameConfigService config) {
        this.config = config;
    }

    @Override
    public String grant(long userId, long roundId, boolean won) {
        GameConfig.Reward r = config.get().reward();
        if (r == null || !r.enabled()) {
            return null;
        }

        int chance = won ? r.dropChanceOnWin() : r.dropChanceOnLoss();
        if (ThreadLocalRandom.current().nextInt(100) >= chance) {
            return null;
        }

        int piece = ThreadLocalRandom.current().nextInt(r.collectionSize()) + 1;
        return "Фрагмент " + piece + " из " + r.collectionSize();
    }

    @Override
    public String name() {
        return "collection (фрагменты коллекции)";
    }
}
