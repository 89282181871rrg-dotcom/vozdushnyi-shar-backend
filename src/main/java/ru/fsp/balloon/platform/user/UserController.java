package ru.fsp.balloon.platform.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Пользователь, баланс и журнал транзакций. */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователь и баланс", description = "Демо-пользователь, бонусный баланс, журнал операций")
public class UserController {

    private final WalletService wallet;

    public UserController(WalletService wallet) {
        this.wallet = wallet;
    }

    @GetMapping("/{username}")
    @Operation(
            summary = "Пользователь и его баланс",
            description = "Демонстрационный пользователь называется demo и создаётся при старте "
                    + "с балансом из config/game-config.yml, секция demo."
    )
    public Map<String, Object> user(@PathVariable String username) {
        return view(wallet.byUsername(username));
    }

    @PostMapping("/{username}/topup")
    @Operation(
            summary = "Пополнить баланс",
            description = "Способ получить бонусные баллы для проверки. "
                    + "Пример: POST /api/users/demo/topup?amount=1000"
    )
    public Map<String, Object> topUp(@PathVariable String username,
                                     @RequestParam long amount) {
        User user = wallet.byUsername(username);
        return view(wallet.topUp(user.getId(), amount));
    }

    @GetMapping("/{username}/transactions")
    @Operation(
            summary = "Журнал операций с бонусами",
            description = "Каждая запись хранит баланс после операции — состояние счёта "
                    + "можно проверить, не пересчитывая всю историю."
    )
    public Map<String, Object> transactions(@PathVariable String username,
                                            @RequestParam(defaultValue = "50") int limit) {
        User user = wallet.byUsername(username);
        List<Map<String, Object>> items = wallet.history(user.getId(), limit).stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("type", t.getType());
                    m.put("amount", t.getAmount());
                    m.put("balanceAfter", t.getBalanceAfter());
                    m.put("comment", t.getComment());
                    m.put("roundId", t.getRoundId());
                    m.put("createdAt", t.getCreatedAt());
                    return m;
                })
                .toList();
        return Map.of("username", username, "items", items);
    }

    private Map<String, Object> view(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("bonusBalance", user.getBonusBalance());
        m.put("points", user.getPoints());
        return m;
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> insufficient(InsufficientBalanceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "insufficient_balance",
                "message", "Не хватает бонусов",
                "balance", e.getBalance(),
                "required", e.getRequired()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "bad_request",
                "message", e.getMessage()));
    }
}
