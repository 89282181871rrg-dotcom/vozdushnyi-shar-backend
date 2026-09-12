package ru.fsp.balloon.platform.round;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.balloon.platform.config.GameConfig;
import ru.fsp.balloon.platform.config.GameConfigService;
import ru.fsp.balloon.platform.events.EventLogService;
import ru.fsp.balloon.platform.extension.RewardProvider;
import ru.fsp.balloon.platform.history.HistoryService;
import ru.fsp.balloon.platform.user.User;
import ru.fsp.balloon.platform.user.WalletService;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class RoundService {

    private final RoundRepository rounds;
    private final GameConfigService config;
    private final WalletService wallet;
    private final HistoryService history;
    private final RewardProvider rewards;
    private final EventLogService events;
    private final ObjectMapper json;
    private final SecureRandom random = new SecureRandom();

    public RoundService(RoundRepository rounds, GameConfigService config, WalletService wallet,
                        HistoryService history, RewardProvider rewards, EventLogService events,
                        ObjectMapper json) {
        this.rounds = rounds;
        this.config = config;
        this.wallet = wallet;
        this.history = history;
        this.rewards = rewards;
        this.events = events;
        this.json = json;
    }

    @Transactional
    public RoundResult create(String username, String themeName, int betId) {
        GameConfig snapshot = config.get();
        if (snapshot.game() == null || !snapshot.game().active()) {
            throw new IllegalStateException("Игра отключена");
        }

        User user = wallet.byUsername(username);
        GameConfig.Bet bet = snapshot.bet(betId);
        GameConfig.Theme theme = snapshot.theme(themeName);
        Instant now = Instant.now();
        double crash = crashPoint(snapshot.math());
        int boosterLevel = selectBoosterLevel(theme, bet);

        Round round = new Round(
                user.getId(), user.getUsername(), themeName, bet.id(), bet.cost(), now,
                crash, snapshot.math().multiplierGrowthRate(), snapshot.math().maxMultiplier(),
                levelsJson(theme), bet.boosterMultiplier(), boosterLevel,
                snapshot.points().perLevel(), snapshot.points().cashoutBonus(), snapshot.points().boosterBonus());
        round = rounds.saveAndFlush(round);
        wallet.placeBet(user.getId(), bet.cost(), round.getId());

        events.log("round_started", Map.of(
                "roundId", round.getId(), "userId", user.getId(), "theme", themeName,
                "bet", bet.cost(), "boosterActivationLevel", boosterLevel));
        return result(round, multiplierAt(round, now), "Раунд запущен");
    }

    @Transactional
    public RoundResult state(Long id) {
        Round round = locked(id);
        if (round.getState() == RoundState.RUNNING && isCrashed(round, Instant.now())) {
            return crashLocked(round, Instant.now());
        }
        return result(round, visibleMultiplier(round, Instant.now()), "Текущее состояние раунда");
    }

    @Transactional
    public RoundResult cashout(Long id) {
        Round round = locked(id);
        if (round.getState() != RoundState.RUNNING) {
            return result(round, terminalMultiplier(round), "Операция уже выполнена");
        }

        Instant now = Instant.now();
        if (isCrashed(round, now)) {
            return crashLocked(round, now);
        }

        double baseMultiplier = multiplierAt(round, now);
        int level = levelReached(round, baseMultiplier);
        boolean boosterHit = round.getBoosterMultiplier() > 1.0
                && level >= round.getBoosterActivationLevel();
        double fixedMultiplier = baseMultiplier * (boosterHit ? round.getBoosterMultiplier() : 1.0);
        long payout = Math.round(round.getBetAmount() * fixedMultiplier);
        long points = points(round, level, true, boosterHit);
        String reward = rewards.grant(round.getUserId(), round.getId(), true);

        wallet.payout(round.getUserId(), payout, round.getId(), "Выигрыш за cashout");
        wallet.addPoints(round.getUserId(), points);
        round.cashOut(now, fixedMultiplier, payout, points, reward, boosterHit);
        rounds.save(round);
        history.record(round.getId(), round.getUserId(), round.getUsername(), round.getTheme(),
                round.getBetAmount(), fixedMultiplier, round.getCrashMultiplier(), payout,
                points, reward, boosterHit, true);
        logCashout(round, fixedMultiplier, boosterHit);
        return result(round, fixedMultiplier, "Выигрыш зафиксирован");
    }

    @Transactional
    public RoundResult settle(Long id) {
        Round round = locked(id);
        if (round.getState() != RoundState.RUNNING) {
            return result(round, terminalMultiplier(round), "Раунд уже завершён");
        }
        Instant now = Instant.now();
        if (!isCrashed(round, now)) {
            return result(round, multiplierAt(round, now), "Раунд ещё не достиг точки краха");
        }
        return crashLocked(round, now);
    }

    private RoundResult crashLocked(Round round, Instant now) {
        int level = levelReached(round, round.getCrashMultiplier());
        long points = points(round, level, false, false);
        String reward = rewards.grant(round.getUserId(), round.getId(), false);
        wallet.addPoints(round.getUserId(), points);
        round.crash(now, points, reward);
        rounds.save(round);
        history.record(round.getId(), round.getUserId(), round.getUsername(), round.getTheme(),
                round.getBetAmount(), null, round.getCrashMultiplier(), 0, points,
                reward, false, false);
        events.log("crash", Map.of("roundId", round.getId(), "level", level));
        return result(round, round.getCrashMultiplier(), "Шар лопнул до cashout");
    }

    private Round locked(Long id) {
        return rounds.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Раунд не найден: id=" + id));
    }

    private double crashPoint(GameConfig.Math math) {
        double u = random.nextDouble();
        double value = math.minCrashMultiplier()
                / java.lang.Math.pow(1.0 - u * (1.0 - math.houseEdge()), math.alpha());
        return java.lang.Math.min(math.maxMultiplier(), java.lang.Math.max(math.minCrashMultiplier(), value));
    }

    private int selectBoosterLevel(GameConfig.Theme theme, GameConfig.Bet bet) {
        if (!bet.hasBooster()) {
            return 0;
        }
        double[] weights = theme.normalizedLootWeights();
        double pick = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (pick < cumulative) {
                return i + 1;
            }
        }
        return weights.length;
    }

    private String levelsJson(GameConfig.Theme theme) {
        try {
            return json.writeValueAsString(theme.levelMultipliers());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сохранить снимок уровней", e);
        }
    }

    private List<Double> levels(Round round) {
        try {
            return json.readValue(round.getLevelMultipliersJson(), new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Повреждён снимок уровней раунда " + round.getId(), e);
        }
    }

    private double multiplierAt(Round round, Instant now) {
        double seconds = Duration.between(round.getStartedAt(), now).toNanos() / 1_000_000_000.0;
        return java.lang.Math.min(round.getMaxMultiplier(), java.lang.Math.exp(round.getGrowthRate() * java.lang.Math.max(0, seconds)));
    }

    private boolean isCrashed(Round round, Instant now) {
        return multiplierAt(round, now) >= round.getCrashMultiplier();
    }

    private double visibleMultiplier(Round round, Instant now) {
        if (round.getState() == RoundState.CRASHED) {
            return round.getCrashMultiplier();
        }
        if (round.getState() == RoundState.CASHED_OUT) {
            return round.getCashoutMultiplier();
        }
        return java.lang.Math.min(multiplierAt(round, now), round.getCrashMultiplier());
    }

    private double terminalMultiplier(Round round) {
        return round.getState() == RoundState.CASHED_OUT
                ? round.getCashoutMultiplier() : round.getCrashMultiplier();
    }

    private int levelReached(Round round, double multiplier) {
        int reached = 0;
        for (double bound : levels(round)) {
            if (multiplier >= bound) {
                reached++;
            } else {
                break;
            }
        }
        return reached;
    }

    private long points(Round round, int level, boolean cashout, boolean boosterHit) {
        return level * round.getPointsPerLevel()
                + (cashout ? round.getCashoutBonusPoints() : 0)
                + (boosterHit ? round.getBoosterBonusPoints() : 0);
    }

    private RoundResult result(Round round, double multiplier, String message) {
        return new RoundResult(round.getId(), round.getState(), multiplier, round.getPayout(),
                round.getPointsEarned(), round.isBoosterHit(), round.getReward(), message);
    }

    private void logCashout(Round round, double multiplier, boolean boosterHit) {
        events.log("cashout", Map.of(
                "roundId", round.getId(), "multiplier", multiplier, "boosterHit", boosterHit));
        if (boosterHit) {
            events.log("booster_hit", Map.of("roundId", round.getId(), "level", round.getBoosterActivationLevel()));
        }
    }
}
