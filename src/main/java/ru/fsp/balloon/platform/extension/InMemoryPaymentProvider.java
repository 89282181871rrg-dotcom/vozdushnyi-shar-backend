package ru.fsp.balloon.platform.extension;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Реализация по умолчанию: операции имитируются, внешние системы не вызываются.
 *
 * <p>Заменяется на реальную платёжку добавлением своего бина —
 * этот помечен {@code @ConditionalOnMissingBean} на уровне конфигурации.
 */
@Component
public class InMemoryPaymentProvider implements PaymentProvider {

    @Override
    public String charge(long userId, long amount, String reason) {
        return "sim-" + UUID.randomUUID();
    }

    @Override
    public String credit(long userId, long amount, String reason) {
        return "sim-" + UUID.randomUUID();
    }

    @Override
    public String name() {
        return "in-memory (имитация, внешних вызовов нет)";
    }
}
