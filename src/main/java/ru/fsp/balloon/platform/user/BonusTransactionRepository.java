package ru.fsp.balloon.platform.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BonusTransactionRepository extends JpaRepository<BonusTransaction, Long> {

    Page<BonusTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
