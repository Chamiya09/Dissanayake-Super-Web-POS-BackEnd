package com.dissayakesuper.web_pos_backend.shift.repository;

import com.dissayakesuper.web_pos_backend.shift.entity.Shift;
import com.dissayakesuper.web_pos_backend.shift.entity.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findFirstByUserIdAndStatusOrderByStartTimeDesc(Long userId, ShiftStatus status);

    List<Shift> findByUserIdOrderByStartTimeDesc(Long userId);

    List<Shift> findAllByOrderByStartTimeDesc();

    long countByStatus(ShiftStatus status);
}
