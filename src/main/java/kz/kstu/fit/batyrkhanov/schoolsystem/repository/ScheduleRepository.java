package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByClassName(String className);

    List<Schedule> findByClassNameOrderByDayOfWeekAscStartTimeAsc(String className);

    @Query("SELECT s FROM Schedule s " +
           "JOIN FETCH s.subject " +
           "JOIN FETCH s.teacher t " +
           "JOIN FETCH t.user " +
           "WHERE s.className = :className " +
           "ORDER BY s.dayOfWeek ASC, s.startTime ASC")
    List<Schedule> findByClassNameWithDetails(@Param("className") String className);
}

