package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Teacher findById(long id);

    @Query("SELECT DISTINCT t FROM Teacher t " +
           "JOIN FETCH t.user " +
           "LEFT JOIN FETCH t.subjects")
    List<Teacher> findAllWithUserAndSubjects();
}