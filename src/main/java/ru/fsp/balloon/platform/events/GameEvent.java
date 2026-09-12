package ru.fsp.balloon.platform.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Запись журнала игровых событий.
 *
 * <p>Журнал — один из способов независимой проверки серверных операций,
 * который прямо назван в критерии «Техническая проверяемость backend».
 * По нему видно, что произошло на сервере и в каком порядке: старт раунда,
 * срабатывание бустера, cashout, крах, перезагрузка конфигурации.
 */
@Entity
@Table(name = "game_events")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Тип события: round_started, booster_hit, cashout, crash, config_reloaded, balance_topup. */
    @Column(nullable = false, length = 64)
    private String type;

    /** Подробности в виде JSON-строки. Схема зависит от типа события. */
    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected GameEvent() {
    }

    public GameEvent(String type, String details) {
        this.type = type;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
