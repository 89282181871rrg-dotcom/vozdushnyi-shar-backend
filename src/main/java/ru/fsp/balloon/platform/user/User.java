package ru.fsp.balloon.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Игрок и его бонусный баланс.
 *
 * <p>Баланс хранится в базе, а не в памяти процесса — как требует постановка.
 *
 * <p>Поле {@code version} включает оптимистическую блокировку. Без неё два
 * одновременных списания могли бы увести баланс в минус: оба потока прочитали
 * бы одно значение и оба записали бы своё. С версией второй записи не будет,
 * операция повторится на свежих данных.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** Бонусные баллы. Целые: дробных бонусов не бывает. */
    @Column(nullable = false)
    private long bonusBalance;

    /** Игровые очки. Отдельная валюта, на неё нельзя играть. */
    @Column(nullable = false)
    private long points;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Version
    private Long version;

    protected User() {
    }

    public User(String username, long startingBalance) {
        this.username = username;
        this.bonusBalance = startingBalance;
        this.points = 0;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public long getBonusBalance() {
        return bonusBalance;
    }

    public long getPoints() {
        return points;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Хватает ли баланса на ставку. Проверяется на сервере, а не скрытием кнопки. */
    public boolean canAfford(long amount) {
        return bonusBalance >= amount;
    }

    void withdraw(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Сумма списания не может быть отрицательной");
        }
        if (!canAfford(amount)) {
            throw new InsufficientBalanceException(bonusBalance, amount);
        }
        this.bonusBalance -= amount;
    }

    void deposit(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Сумма начисления не может быть отрицательной");
        }
        this.bonusBalance += amount;
    }

    void addPoints(long amount) {
        this.points += amount;
    }
}
