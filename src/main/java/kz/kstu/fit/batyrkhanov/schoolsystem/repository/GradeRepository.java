package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Grade;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudent(Student student);

    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH g.subject " +
           "JOIN FETCH g.teacher t " +
           "JOIN FETCH t.user " +
           "WHERE g.student = :student " +
           "ORDER BY g.gradeDate DESC")
    List<Grade> findByStudentOrderByGradeDateDesc(@Param("student") Student student);

    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH g.subject subj " +
           "WHERE g.teacher.id = :teacherId AND s.className = :className " +
           "ORDER BY g.gradeDate DESC, s.user.fullName ASC")
    List<Grade> findByTeacherIdAndClassNameWithDetails(@Param("teacherId") Long teacherId,
                                                       @Param("className") String className);

    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH g.subject subj " +
           "JOIN FETCH g.teacher t " +
           "JOIN FETCH t.user " +
           "WHERE g.id = :id")
    Optional<Grade> findByIdWithAll(@Param("id") Long id);
}
