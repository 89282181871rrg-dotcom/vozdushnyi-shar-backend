package ru.fsp.balloon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Воздушный Шар — бонусная crash-игра.
 *
 * <p>Модуль «Платформа, конфигурация и проверяемость»: всё, что нужно, чтобы
 * эксперт запустил сервис и прошёл обязательные сценарии в одиночку, не
 * обращаясь к команде.
 */
@SpringBootApplication
@EnableScheduling
public class BalloonApplication {
    public static void main(String[] args) {
        SpringApplication.run(BalloonApplication.class, args);
    }
}
