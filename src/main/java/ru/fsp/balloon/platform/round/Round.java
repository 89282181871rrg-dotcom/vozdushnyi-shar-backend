package ru.fsp.balloon.platform.round;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 32)
    private String theme;

    @Column(nullable = false)
    private int betId;

    @Column(nullable = false)
    private long betAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoundState state;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant startedAt;

    @Column
    private Instant finishedAt;

    /** Server-only value. It is deliberately never exposed by the controller. */
    @Column(nullable = false)
    private double crashMultiplier;

    @Column(nullable = false)
    private double growthRate;

    @Column(nullable = false)
    private double maxMultiplier;

    @Lob
    @Column(nullable = false)
    private String levelMultipliersJson;

    @Column(nullable = false)
    private double boosterMultiplier;

    @Column(nullable = false)
    private int boosterActivationLevel;

    @Column
    private Double cashoutMultiplier;

    @Column(nullable = false)
    private long payout;

    @Column(nullable = false)
    private long pointsEarned;

    @Column(length = 255)
    private String reward;

    @Column(nullable = false)
    private boolean boosterHit;

    @Column(nullable = false)
    private long pointsPerLevel;

    @Column(nullable = false)
    private long cashoutBonusPoints;

    @Column(nullable = false)
    private long boosterBonusPoints;

    @Version
    private Long version;

    protected Round() {
    }

    public Round(Long userId, String username, String theme, int betId, long betAmount,
                 Instant now, double crashMultiplier, double growthRate, double maxMultiplier,
                 String levelMultipliersJson, double boosterMultiplier, int boosterActivationLevel,
                 long pointsPerLevel, long cashoutBonusPoints, long boosterBonusPoints) {
        this.userId = userId;
        this.username = username;
        this.theme = theme;
        this.betId = betId;
        this.betAmount = betAmount;
        this.state = RoundState.RUNNING;
        this.createdAt = now;
        this.startedAt = now;
        this.crashMultiplier = crashMultiplier;
        this.growthRate = growthRate;
        this.maxMultiplier = maxMultiplier;
        this.levelMultipliersJson = levelMultipliersJson;
        this.boosterMultiplier = boosterMultiplier;
        this.boosterActivationLevel = boosterActivationLevel;
        this.pointsPerLevel = pointsPerLevel;
        this.cashoutBonusPoints = cashoutBonusPoints;
        this.boosterBonusPoints = boosterBonusPoints;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getTheme() { return theme; }
    public int getBetId() { return betId; }
    public long getBetAmount() { return betAmount; }
    public RoundState getState() { return state; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public double getCrashMultiplier() { return crashMultiplier; }
    public double getGrowthRate() { return growthRate; }
    public double getMaxMultiplier() { return maxMultiplier; }
    public String getLevelMultipliersJson() { return levelMultipliersJson; }
    public double getBoosterMultiplier() { return boosterMultiplier; }
    public int getBoosterActivationLevel() { return boosterActivationLevel; }
    public Double getCashoutMultiplier() { return cashoutMultiplier; }
    public long getPayout() { return payout; }
    public long getPointsEarned() { return pointsEarned; }
    public String getReward() { return reward; }
    public boolean isBoosterHit() { return boosterHit; }
    public long getPointsPerLevel() { return pointsPerLevel; }
    public long getCashoutBonusPoints() { return cashoutBonusPoints; }
    public long getBoosterBonusPoints() { return boosterBonusPoints; }

    void cashOut(Instant finishedAt, double multiplier, long payout, long points,
                 String reward, boolean boosterHit) {
        this.state = RoundState.CASHED_OUT;
        this.finishedAt = finishedAt;
        this.cashoutMultiplier = multiplier;
        this.payout = payout;
        this.pointsEarned = points;
        this.reward = reward;
        this.boosterHit = boosterHit;
    }

    void crash(Instant finishedAt, long points, String reward) {
        this.state = RoundState.CRASHED;
        this.finishedAt = finishedAt;
        this.pointsEarned = points;
        this.reward = reward;
    }
}
