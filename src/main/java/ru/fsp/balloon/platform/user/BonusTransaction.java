package ru.fsp.balloon.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Журнал движения бонусных баллов.
 *
 * <p>Хранит не только сумму, но и баланс после операции. Благодаря этому
 * любое состояние баланса можно проверить по журналу, не пересчитывая всю
 * историю: критерий требует журнал транзакций как способ проверки.
 */
@Entity
@Table(name = "bonus_transactions")
public class BonusTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Со знаком: списание отрицательное, начисление положительное. */
    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long balanceAfter;

    /** BET, WIN, TOPUP, REWARD, UPSELL. */
    @Column(nullable = false, length = 32)
    private String type;

    @Column(length = 255)
    private String comment;

    @Column
    private Long roundId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected BonusTransaction() {
    }

    public BonusTransaction(Long userId, long amount, long balanceAfter,
                            String type, String comment, Long roundId) {
        this.userId = userId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.type = type;
        this.comment = comment;
        this.roundId = roundId;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public String getType() {
        return type;
    }

    public String getComment() {
        return comment;
    }

    public Long getRoundId() {
        return roundId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
