package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Director;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Subject findByName(String name);
}