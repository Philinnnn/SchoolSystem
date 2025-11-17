package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Director;

@Repository
public interface DirectorRepository extends JpaRepository<Director, Long> {
    Director findByUserId(Long userId);
}

