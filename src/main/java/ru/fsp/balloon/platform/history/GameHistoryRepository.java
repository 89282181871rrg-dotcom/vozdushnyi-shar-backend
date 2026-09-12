package ru.fsp.balloon.platform.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameHistoryRepository extends JpaRepository<GameHistoryEntry, Long> {

    Page<GameHistoryEntry> findAllByOrderByFinishedAtDesc(Pageable pageable);

    Page<GameHistoryEntry> findByUsernameOrderByFinishedAtDesc(String username, Pageable pageable);
}
