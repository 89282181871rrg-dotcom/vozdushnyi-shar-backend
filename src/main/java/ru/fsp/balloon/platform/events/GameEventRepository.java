package ru.fsp.balloon.platform.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    Page<GameEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<GameEvent> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);
}
