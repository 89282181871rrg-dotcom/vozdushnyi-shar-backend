package ru.fsp.balloon.platform.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Модель игровой конфигурации — ровно то, что лежит в config/game-config.yml.
 *
 * <p>Неизменяемая: после загрузки объект не меняется. Новая версия конфига —
 * это новый объект, который атомарно подменяет прежний. Благодаря этому раунд,
 * начатый на старой конфигурации, доигрывает по ней же, и результат остаётся
 * воспроизводимым.
 *
 * <p>Валидация вынесена в {@link GameConfigValidator}: модель только описывает
 * форму данных и ничего не проверяет сама.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameConfig(
        Game game,
        List<Bet> bets,
        Map<String, Theme> themes,
        Math math,
        Points points,
        Reward reward,
        Round round,
        Upsell upsell,
        Demo demo
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Game(String id, String name, boolean active) {}

    /** Один фрагмент пазла на экране выбора ставки. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bet(int id, long cost, double boosterMultiplier) {
        /** Фрагмент без усиления — маркер бустера на поле не появляется. */
        public boolean hasBooster() {
            return boosterMultiplier > 1.0;
        }
    }

    /**
     * Тема игры: зелёная (9 уровней) или красная (12).
     *
     * @param levelMultipliers коэффициент на границе каждого уровня, строго возрастает
     * @param lootProbabilities веса выпадения бустера по уровням, нормализуются автоматически
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Theme(int levels, List<Double> levelMultipliers, List<Double> lootProbabilities) {

        /** Коэффициент на границе уровня. Нумерация уровней с единицы. */
        public double multiplierAtLevel(int level) {
            if (level < 1) {
                return 1.0;
            }
            int idx = java.lang.Math.min(level, levelMultipliers.size()) - 1;
            return levelMultipliers.get(idx);
        }

        /**
         * Сколько уровней пройдено при данном коэффициенте.
         * Используется и для начисления очков, и для проверки, доступен ли cashout.
         */
        public int levelReached(double multiplier) {
            int reached = 0;
            for (double bound : levelMultipliers) {
                if (multiplier >= bound) {
                    reached++;
                } else {
                    break;
                }
            }
            return reached;
        }

        /** Веса бустера, приведённые к сумме 1. */
        public double[] normalizedLootWeights() {
            double sum = lootProbabilities.stream().mapToDouble(Double::doubleValue).sum();
            double[] out = new double[lootProbabilities.size()];
            if (sum <= 0) {
                java.util.Arrays.fill(out, 1.0 / out.length);
                return out;
            }
            for (int i = 0; i < out.length; i++) {
                out[i] = lootProbabilities.get(i) / sum;
            }
            return out;
        }
    }

    /** Параметры распределения точки краха и роста коэффициента. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Math(
            double alpha,
            double minCrashMultiplier,
            double maxMultiplier,
            double multiplierGrowthRate,
            int fps,
            double houseEdge
    ) {}

    /** Начисление игровых очков. Очки — не бонусные баллы, это разные валюты. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Points(long perLevel, long cashoutBonus, long boosterBonus) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reward(
            boolean enabled,
            int collectionSize,
            long bonusForFullSet,
            int dropChanceOnWin,
            int dropChanceOnLoss
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Round(int idleTimeoutSeconds, int autoSettleAfterSeconds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Upsell(
            boolean enabled,
            long minWinAmount,
            int popupTimeoutSeconds,
            boolean oncePerSession
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Demo(String username, long startingBalance) {}

    /** Тема по имени. Неизвестное имя — ошибка запроса, а не молчаливая подстановка. */
    public Theme theme(String name) {
        Theme t = themes.get(name);
        if (t == null) {
            throw new IllegalArgumentException(
                    "Неизвестная тема: " + name + ". Доступны: " + themes.keySet());
        }
        return t;
    }

    /** Вариант ставки по идентификатору фрагмента. */
    public Bet bet(int betId) {
        return bets.stream()
                .filter(b -> b.id() == betId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный вариант ставки: " + betId));
    }
}
