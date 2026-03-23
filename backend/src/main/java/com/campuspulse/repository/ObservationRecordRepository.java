package com.campuspulse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.campuspulse.model.ObservationRecord;
import com.campuspulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationRecordRepository extends JpaRepository<ObservationRecord, Long> {

    List<ObservationRecord> findByUserAndEffectiveDateTimeBetweenOrderByEffectiveDateTimeAsc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    List<ObservationRecord> findByUserOrderByEffectiveDateTimeAsc(User user);

    Optional<ObservationRecord> findTopByUserOrderByEffectiveDateTimeDesc(User user);
}
