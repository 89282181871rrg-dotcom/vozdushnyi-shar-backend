package ru.fsp.balloon.platform.round;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Round r where r.id = :id")
    Optional<Round> findByIdForUpdate(@Param("id") Long id);
}
