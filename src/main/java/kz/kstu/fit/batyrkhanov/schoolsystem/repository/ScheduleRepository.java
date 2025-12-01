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

    @Query("SELECT s FROM Schedule s " +
           "JOIN FETCH s.subject " +
           "JOIN FETCH s.teacher t " +
           "JOIN FETCH t.user " +
           "WHERE t.id = :teacherId " +
           "ORDER BY s.dayOfWeek ASC, s.startTime ASC")
    List<Schedule> findByTeacherIdWithDetails(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT s.className FROM Schedule s WHERE s.teacher.id = :teacherId ORDER BY s.className ASC")
    List<String> findDistinctClassNamesByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT s.className FROM Schedule s ORDER BY s.className ASC")
    List<String> findDistinctClassNamesGlobal();

    @Query("SELECT COUNT(s) > 0 FROM Schedule s WHERE s.teacher.id = :teacherId AND s.subject.id = :subjectId AND s.className = :className AND s.dayOfWeek = :dow")
    boolean existsLessonForTeacherSubjectClassOnDay(@Param("teacherId") Long teacherId,
                                                    @Param("subjectId") Long subjectId,
                                                    @Param("className") String className,
                                                    @Param("dow") DayOfWeek dayOfWeek);

    @Query("SELECT s FROM Schedule s " +
           "JOIN FETCH s.subject " +
           "JOIN FETCH s.teacher t " +
           "JOIN FETCH t.user " +
           "ORDER BY s.className ASC, s.dayOfWeek ASC, s.startTime ASC")
    List<Schedule> findAllWithDetails();

    List<Schedule> findByClassNameAndDayOfWeek(String className, DayOfWeek dayOfWeek);

    List<Schedule> findByTeacherIdAndDayOfWeek(Long teacherId, DayOfWeek dayOfWeek);
}
