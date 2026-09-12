package ru.fsp.balloon.platform.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /**
     * Загрузка с блокировкой строки на время транзакции.
     *
     * <p>Нужна там, где баланс меняется: списание ставки и выплата выигрыша.
     * Без неё два параллельных запроса могут прочитать один и тот же баланс
     * и оба списать с него — классическая гонка.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
