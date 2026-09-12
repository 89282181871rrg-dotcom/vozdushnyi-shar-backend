package ru.fsp.balloon.platform.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Завершённый раунд в истории игр.
 *
 * <p>История обязательна по постановке и показывается на экране выбора ставки —
 * с результатами всех пользователей прототипа, а не только текущего.
 * От наличия живого рейтинга и турниров не зависит.
 */
@Entity
@Table(name = "game_history")
public class GameHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roundId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 16)
    private String theme;

    @Column(nullable = false)
    private long bet;

    /** Коэффициент, на котором игрок забрал выигрыш. null, если не успел. */
    @Column
    private Double cashoutMultiplier;

    /** Коэффициент, на котором лопнул шар. Виден всегда, в том числе при выигрыше. */
    @Column(nullable = false)
    private double crashMultiplier;

    @Column(nullable = false)
    private long payout;

    @Column(nullable = false)
    private long pointsEarned;

    @Column(length = 255)
    private String reward;

    @Column(nullable = false)
    private boolean boosterHit;

    @Column(nullable = false)
    private boolean won;

    @Column(nullable = false)
    private Instant finishedAt = Instant.now();

    protected GameHistoryEntry() {
    }

    public GameHistoryEntry(Long roundId, Long userId, String username, String theme,
                            long bet, Double cashoutMultiplier, double crashMultiplier,
                            long payout, long pointsEarned, String reward,
                            boolean boosterHit, boolean won) {
        this.roundId = roundId;
        this.userId = userId;
        this.username = username;
        this.theme = theme;
        this.bet = bet;
        this.cashoutMultiplier = cashoutMultiplier;
        this.crashMultiplier = crashMultiplier;
        this.payout = payout;
        this.pointsEarned = pointsEarned;
        this.reward = reward;
        this.boosterHit = boosterHit;
        this.won = won;
        this.finishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getRoundId() { return roundId; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getTheme() { return theme; }
    public long getBet() { return bet; }
    public Double getCashoutMultiplier() { return cashoutMultiplier; }
    public double getCrashMultiplier() { return crashMultiplier; }
    public long getPayout() { return payout; }
    public long getPointsEarned() { return pointsEarned; }
    public String getReward() { return reward; }
    public boolean isBoosterHit() { return boosterHit; }
    public boolean isWon() { return won; }
    public Instant getFinishedAt() { return finishedAt; }
}
