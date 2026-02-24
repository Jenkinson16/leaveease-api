package com.leaveease.leaveease_api.repository;

import com.leaveease.leaveease_api.entity.LeaveRequest;
import com.leaveease.leaveease_api.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByUserId(Long userId);

    List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LeaveRequest> findByIdAndUserId(Long id, Long userId);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    @Query("""
            SELECT COUNT(lr) > 0 FROM LeaveRequest lr
            WHERE lr.user.id = :userId
              AND lr.status IN (:statuses)
              AND lr.startDate <= :endDate
              AND lr.endDate >= :startDate
            """)
    boolean existsOverlap(@Param("userId") Long userId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate,
                          @Param("statuses") List<LeaveStatus> statuses);
}
