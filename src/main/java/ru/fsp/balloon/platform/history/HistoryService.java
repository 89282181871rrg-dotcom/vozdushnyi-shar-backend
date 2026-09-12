package ru.fsp.balloon.platform.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * История завершённых раундов.
 *
 * <p>Игровое ядро вызывает {@link #record} при закрытии раунда. Всё, что
 * нужно знать этому модулю, приходит параметрами — прямой зависимости
 * от движка нет, поэтому его можно писать и тестировать отдельно.
 */
@Service
public class HistoryService {

    private final GameHistoryRepository repository;

    public HistoryService(GameHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GameHistoryEntry record(Long roundId, Long userId, String username, String theme,
                                   long bet, Double cashoutMultiplier, double crashMultiplier,
                                   long payout, long pointsEarned, String reward,
                                   boolean boosterHit, boolean won) {
        return repository.save(new GameHistoryEntry(
                roundId, userId, username, theme, bet, cashoutMultiplier, crashMultiplier,
                payout, pointsEarned, reward, boosterHit, won));
    }

    @Transactional(readOnly = true)
    public Page<GameHistoryEntry> recent(String username, int page, int size) {
        PageRequest request = PageRequest.of(page, java.lang.Math.min(size, 200));
        return username == null || username.isBlank()
                ? repository.findAllByOrderByFinishedAtDesc(request)
                : repository.findByUsernameOrderByFinishedAtDesc(username, request);
    }
}
