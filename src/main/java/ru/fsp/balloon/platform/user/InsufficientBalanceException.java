package ru.fsp.balloon.platform.user;

/**
 * Не хватает бонусных баллов на ставку.
 *
 * <p>Проверка серверная: клиент может сколько угодно рисовать активную кнопку,
 * ставка всё равно не пройдёт. Это отдельно проверяется в сценарии 1.
 */
public class InsufficientBalanceException extends RuntimeException {

    private final long balance;
    private final long required;

    public InsufficientBalanceException(long balance, long required) {
        super("Не хватает бонусов: на балансе " + balance + ", нужно " + required);
        this.balance = balance;
        this.required = required;
    }

    public long getBalance() {
        return balance;
    }

    public long getRequired() {
        return required;
    }
}
