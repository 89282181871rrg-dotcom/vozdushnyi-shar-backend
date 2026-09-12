package ru.fsp.balloon.platform.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Проверка игровой конфигурации перед применением.
 *
 * <p>Смысл: человек правит YAML руками и обязательно когда-нибудь ошибётся.
 * Сервис не должен падать и не должен молча играть по сломанным правилам.
 * Поэтому конфигурация проверяется до применения, и при любой ошибке
 * остаётся в силе прошлая рабочая версия.
 *
 * <p>Сообщения пишутся так, чтобы по ним сразу было понятно, что править —
 * с указанием пути до поля и допустимых значений.
 */
@Component
public class GameConfigValidator {

    /** Количество вариантов ставки задано макетом экрана: четыре фрагмента пазла. */
    private static final int REQUIRED_BET_COUNT = 4;

    /** Количество уровней для каждой темы задано постановкой задачи. */
    private static final Map<String, Integer> REQUIRED_LEVELS = Map.of("green", 9, "red", 12);

    /**
     * @return список ошибок; пустой список означает, что конфигурацию можно применять
     */
    public List<String> validate(GameConfig c) {
        List<String> errors = new ArrayList<>();

        if (c == null) {
            errors.add("Конфигурация пуста — файл не прочитан или содержит только комментарии");
            return errors;
        }

        validateGame(c, errors);
        validateBets(c, errors);
        validateThemes(c, errors);
        validateMath(c, errors);
        validatePoints(c, errors);
        validateReward(c, errors);
        validateRound(c, errors);
        validateDemo(c, errors);

        return errors;
    }

    private void validateGame(GameConfig c, List<String> errors) {
        if (c.game() == null) {
            errors.add("game: секция отсутствует");
            return;
        }
        if (isBlank(c.game().id())) {
            errors.add("game.id: не задан");
        }
        if (isBlank(c.game().name())) {
            errors.add("game.name: не задано");
        }
    }

    private void validateBets(GameConfig c, List<String> errors) {
        if (c.bets() == null || c.bets().isEmpty()) {
            errors.add("bets: не задан ни один вариант ставки");
            return;
        }
        if (c.bets().size() != REQUIRED_BET_COUNT) {
            errors.add("bets: должно быть ровно " + REQUIRED_BET_COUNT
                    + " варианта (по числу фрагментов пазла на экране), сейчас " + c.bets().size());
        }

        List<Integer> seenIds = new ArrayList<>();
        for (GameConfig.Bet bet : c.bets()) {
            String at = "bets[id=" + bet.id() + "]";
            if (seenIds.contains(bet.id())) {
                errors.add(at + ": идентификатор повторяется, он должен быть уникальным");
            }
            seenIds.add(bet.id());

            if (bet.cost() <= 0) {
                errors.add(at + ".cost: стоимость должна быть больше нуля, сейчас " + bet.cost());
            }
            if (bet.boosterMultiplier() < 1.0 || bet.boosterMultiplier() > 10.0) {
                errors.add(at + ".boosterMultiplier: допустимо от 1 до 10 "
                        + "(1 — фрагмент без бустера), сейчас " + bet.boosterMultiplier());
            }
        }
    }

    private void validateThemes(GameConfig c, List<String> errors) {
        if (c.themes() == null || c.themes().isEmpty()) {
            errors.add("themes: не задана ни одна тема");
            return;
        }

        for (Map.Entry<String, Integer> required : REQUIRED_LEVELS.entrySet()) {
            if (!c.themes().containsKey(required.getKey())) {
                errors.add("themes." + required.getKey() + ": тема обязательна по условию задачи");
            }
        }

        c.themes().forEach((name, theme) -> {
            String at = "themes." + name;

            Integer requiredLevels = REQUIRED_LEVELS.get(name);
            if (requiredLevels != null && theme.levels() != requiredLevels) {
                errors.add(at + ".levels: по условию задачи должно быть " + requiredLevels
                        + ", сейчас " + theme.levels());
            }
            if (theme.levels() <= 0) {
                errors.add(at + ".levels: должно быть больше нуля");
            }

            if (theme.levelMultipliers() == null || theme.levelMultipliers().isEmpty()) {
                errors.add(at + ".levelMultipliers: список не задан");
            } else {
                if (theme.levelMultipliers().size() != theme.levels()) {
                    errors.add(at + ".levelMultipliers: должно быть ровно " + theme.levels()
                            + " значений по числу уровней, сейчас " + theme.levelMultipliers().size());
                }
                double prev = 1.0;
                for (int i = 0; i < theme.levelMultipliers().size(); i++) {
                    double v = theme.levelMultipliers().get(i);
                    if (v <= prev) {
                        errors.add(at + ".levelMultipliers[" + i + "]: значения должны строго "
                                + "возрастать и быть больше 1.0 — " + v + " не больше предыдущего " + prev);
                    }
                    prev = v;
                }
            }

            if (theme.lootProbabilities() == null || theme.lootProbabilities().isEmpty()) {
                errors.add(at + ".lootProbabilities: список не задан");
            } else {
                if (theme.lootProbabilities().size() != theme.levels()) {
                    errors.add(at + ".lootProbabilities: должно быть ровно " + theme.levels()
                            + " значений по числу уровней, сейчас " + theme.lootProbabilities().size());
                }
                for (int i = 0; i < theme.lootProbabilities().size(); i++) {
                    if (theme.lootProbabilities().get(i) < 0) {
                        errors.add(at + ".lootProbabilities[" + i + "]: вес не может быть отрицательным");
                    }
                }
                double sum = theme.lootProbabilities().stream().mapToDouble(Double::doubleValue).sum();
                if (sum <= 0) {
                    errors.add(at + ".lootProbabilities: сумма весов должна быть больше нуля, "
                            + "иначе бустер некуда поставить");
                }
            }
        });
    }

    private void validateMath(GameConfig c, List<String> errors) {
        if (c.math() == null) {
            errors.add("math: секция отсутствует");
            return;
        }
        GameConfig.Math m = c.math();

        if (m.alpha() < 0.1 || m.alpha() > 2.0) {
            errors.add("math.alpha: допустимо от 0.1 до 2.0, сейчас " + m.alpha()
                    + ". Больше значение — чаще длинные полёты");
        }
        if (m.minCrashMultiplier() < 1.0) {
            errors.add("math.minCrashMultiplier: не может быть меньше 1.0, сейчас "
                    + m.minCrashMultiplier());
        }
        if (m.maxMultiplier() <= m.minCrashMultiplier()) {
            errors.add("math.maxMultiplier: должен быть больше minCrashMultiplier ("
                    + m.minCrashMultiplier() + "), сейчас " + m.maxMultiplier());
        }
        if (m.multiplierGrowthRate() <= 0) {
            errors.add("math.multiplierGrowthRate: должен быть больше нуля, иначе коэффициент "
                    + "не растёт и раунд никогда не закончится");
        }
        if (m.fps() < 1 || m.fps() > 120) {
            errors.add("math.fps: допустимо от 1 до 120, сейчас " + m.fps());
        }
        if (m.houseEdge() < 0 || m.houseEdge() >= 1) {
            errors.add("math.houseEdge: доля заведения, допустимо от 0 до 0.99, сейчас "
                    + m.houseEdge());
        }
    }

    private void validatePoints(GameConfig c, List<String> errors) {
        if (c.points() == null) {
            errors.add("points: секция отсутствует");
            return;
        }
        if (c.points().perLevel() < 0) {
            errors.add("points.perLevel: не может быть отрицательным");
        }
        if (c.points().cashoutBonus() < 0) {
            errors.add("points.cashoutBonus: не может быть отрицательным");
        }
        if (c.points().boosterBonus() < 0) {
            errors.add("points.boosterBonus: не может быть отрицательным");
        }
    }

    private void validateReward(GameConfig c, List<String> errors) {
        if (c.reward() == null) {
            errors.add("reward: секция отсутствует");
            return;
        }
        GameConfig.Reward r = c.reward();
        if (r.collectionSize() <= 0) {
            errors.add("reward.collectionSize: должен быть больше нуля");
        }
        if (r.bonusForFullSet() < 0) {
            errors.add("reward.bonusForFullSet: не может быть отрицательным");
        }
        if (r.dropChanceOnWin() < 0 || r.dropChanceOnWin() > 100) {
            errors.add("reward.dropChanceOnWin: проценты, допустимо от 0 до 100, сейчас "
                    + r.dropChanceOnWin());
        }
        if (r.dropChanceOnLoss() < 0 || r.dropChanceOnLoss() > 100) {
            errors.add("reward.dropChanceOnLoss: проценты, допустимо от 0 до 100, сейчас "
                    + r.dropChanceOnLoss());
        }
    }

    private void validateRound(GameConfig c, List<String> errors) {
        if (c.round() == null) {
            errors.add("round: секция отсутствует");
            return;
        }
        if (c.round().idleTimeoutSeconds() <= 0) {
            errors.add("round.idleTimeoutSeconds: должен быть больше нуля");
        }
        if (c.round().autoSettleAfterSeconds() <= 0) {
            errors.add("round.autoSettleAfterSeconds: должен быть больше нуля");
        }
    }

    private void validateDemo(GameConfig c, List<String> errors) {
        if (c.demo() == null) {
            errors.add("demo: секция отсутствует — без демо-пользователя эксперт не пройдёт сценарии");
            return;
        }
        if (isBlank(c.demo().username())) {
            errors.add("demo.username: не задан");
        }
        if (c.demo().startingBalance() < 0) {
            errors.add("demo.startingBalance: не может быть отрицательным");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
