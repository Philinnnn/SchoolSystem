package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Student;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Student findByUserId(Long userId);
    List<Student> findByClassName(String className);
}

