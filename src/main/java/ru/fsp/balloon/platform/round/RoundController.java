package ru.fsp.balloon.platform.round;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.balloon.platform.user.InsufficientBalanceException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/rounds", "/rounds"})
@Tag(name = "Игровое ядро", description = "Серверные раунды, crash-точка, cashout и расчёты")
public class RoundController {

    private final RoundService service;

    public RoundController(RoundService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Создать и запустить раунд",
            description = "Ставка проверяется и списывается на сервере. Crash-точка и бустер определяются до старта.")
    public RoundResult create(@Valid @RequestBody CreateRoundRequest request) {
        return service.create(request.username(), request.theme(), request.betId());
    }

    @GetMapping("/{id}/state")
    @Operation(summary = "Получить серверное состояние раунда",
            description = "Crash-точка никогда не возвращается до завершения раунда.")
    public RoundResult state(@PathVariable Long id) {
        return service.state(id);
    }

    @PostMapping("/{id}/cashout")
    @Operation(summary = "Забрать выигрыш",
            description = "Идемпотентно. Сервер игнорирует время и коэффициент из клиента.")
    public RoundResult cashout(@PathVariable Long id) {
        return service.cashout(id);
    }

    @PostMapping("/{id}/settle")
    @Operation(summary = "Зафиксировать crash",
            description = "Раунд фиксируется только если серверное время достигло сохранённой crash-точки.")
    public ResponseEntity<RoundResult> settle(@PathVariable Long id) {
        RoundResult result = service.settle(id);
        return result.state() == RoundState.RUNNING
                ? ResponseEntity.status(HttpStatus.CONFLICT).body(result)
                : ResponseEntity.ok(result);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> insufficient(InsufficientBalanceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "insufficient_balance", "message", "Не хватает бонусов",
                "balance", e.getBalance(), "required", e.getRequired()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "bad_request", "message", e.getMessage()));
    }

    public record CreateRoundRequest(
            @NotBlank String username,
            @NotBlank String theme,
            @Positive int betId
    ) {
    }
}
