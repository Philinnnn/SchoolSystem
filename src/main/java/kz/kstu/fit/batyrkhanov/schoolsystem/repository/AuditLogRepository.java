package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("SELECT a FROM AuditLog a WHERE (:user IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :user, '%'))) AND (:action IS NULL OR a.action = :action) AND (:from IS NULL OR a.eventTime >= :from) AND (:to IS NULL OR a.eventTime <= :to) ORDER BY a.eventTime DESC")
    Page<AuditLog> search(@Param("user") String user,
                          @Param("action") String action,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
