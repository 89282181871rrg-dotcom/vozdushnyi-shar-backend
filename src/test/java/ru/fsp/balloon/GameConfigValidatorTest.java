package ru.fsp.balloon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.fsp.balloon.platform.config.GameConfig;
import ru.fsp.balloon.platform.config.GameConfigValidator;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка валидатора конфигурации.
 *
 * <p>Смысл этих тестов — доказать, что сломанный конфиг не будет применён.
 * Это прямо поддерживает критерий «Техническая проверяемость backend»
 * и обязательный сценарий 5.
 */
class GameConfigValidatorTest {

    private final GameConfigValidator validator = new GameConfigValidator();

    @Test
    @DisplayName("Корректная конфигурация проходит без ошибок")
    void validConfigPasses() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    @DisplayName("Зелёная тема обязана иметь ровно 9 уровней")
    void greenThemeMustHaveNineLevels() {
        GameConfig broken = withGreenLevels(8);
        List<String> errors = validator.validate(broken);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("themes.green.levels")));
    }

    @Test
    @DisplayName("Коэффициенты уровней должны строго возрастать")
    void levelMultipliersMustIncrease() {
        GameConfig broken = withGreenMultipliers(
                List.of(1.2, 1.1, 1.9, 2.4, 3.1, 4.0, 5.2, 6.8, 9.0));
        assertTrue(validator.validate(broken).stream()
                .anyMatch(e -> e.contains("levelMultipliers")));
    }

    @Test
    @DisplayName("alpha вне допустимого диапазона отклоняется")
    void alphaOutOfRangeRejected() {
        GameConfig broken = withAlpha(5.0);
        assertTrue(validator.validate(broken).stream().anyMatch(e -> e.contains("math.alpha")));
    }

    @Test
    @DisplayName("Должно быть ровно четыре варианта ставки")
    void fourBetsRequired() {
        GameConfig broken = new GameConfig(
                valid().game(),
                List.of(new GameConfig.Bet(1, 10, 1)),
                valid().themes(), valid().math(), valid().points(),
                valid().reward(), valid().round(), valid().upsell(), valid().demo());
        assertTrue(validator.validate(broken).stream().anyMatch(e -> e.contains("bets")));
    }

    @Test
    @DisplayName("Уровень определяется по коэффициенту корректно")
    void levelReachedIsCorrect() {
        GameConfig.Theme green = valid().theme("green");
        assertEquals(0, green.levelReached(1.10));
        assertEquals(1, green.levelReached(1.20));
        assertEquals(2, green.levelReached(1.75));
        assertEquals(9, green.levelReached(50.0));
    }

    // ------------------------------------------------------------------------

    private GameConfig valid() {
        return new GameConfig(
                new GameConfig.Game("balloon", "Воздушный Шар", true),
                List.of(
                        new GameConfig.Bet(1, 10, 1),
                        new GameConfig.Bet(2, 25, 2),
                        new GameConfig.Bet(3, 50, 3),
                        new GameConfig.Bet(4, 100, 4)),
                Map.of(
                        "green", new GameConfig.Theme(9,
                                List.of(1.20, 1.50, 1.90, 2.40, 3.10, 4.00, 5.20, 6.80, 9.00),
                                List.of(4.0, 6.0, 9.0, 12.0, 15.0, 16.0, 15.0, 13.0, 10.0)),
                        "red", new GameConfig.Theme(12,
                                List.of(1.15, 1.40, 1.70, 2.10, 2.60, 3.20, 4.00, 5.00, 6.30, 8.00, 10.50, 14.00),
                                List.of(3.0, 4.0, 6.0, 8.0, 10.0, 12.0, 13.0, 13.0, 12.0, 10.0, 6.0, 3.0))),
                new GameConfig.Math(1.0, 1.0, 100.0, 0.06, 30, 0.03),
                new GameConfig.Points(10, 25, 50),
                new GameConfig.Reward(true, 6, 250, 100, 40),
                new GameConfig.Round(10, 120),
                new GameConfig.Upsell(false, 50, 10, true),
                new GameConfig.Demo("demo", 1000));
    }

    private GameConfig withGreenLevels(int levels) {
        GameConfig v = valid();
        GameConfig.Theme g = v.theme("green");
        return replaceGreen(v, new GameConfig.Theme(levels, g.levelMultipliers(), g.lootProbabilities()));
    }

    private GameConfig withGreenMultipliers(List<Double> multipliers) {
        GameConfig v = valid();
        GameConfig.Theme g = v.theme("green");
        return replaceGreen(v, new GameConfig.Theme(g.levels(), multipliers, g.lootProbabilities()));
    }

    private GameConfig replaceGreen(GameConfig v, GameConfig.Theme green) {
        return new GameConfig(v.game(), v.bets(),
                Map.of("green", green, "red", v.theme("red")),
                v.math(), v.points(), v.reward(), v.round(), v.upsell(), v.demo());
    }

    private GameConfig withAlpha(double alpha) {
        GameConfig v = valid();
        GameConfig.Math m = v.math();
        return new GameConfig(v.game(), v.bets(), v.themes(),
                new GameConfig.Math(alpha, m.minCrashMultiplier(), m.maxMultiplier(),
                        m.multiplierGrowthRate(), m.fps(), m.houseEdge()),
                v.points(), v.reward(), v.round(), v.upsell(), v.demo());
    }
}
