package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Subject;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Director;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Teacher findById(long id);
}