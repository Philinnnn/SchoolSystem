package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Subject findByName(String name);

    @Query("SELECT s FROM Subject s")
    List<Subject> findAllWithTeachers();
}