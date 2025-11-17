package kz.kstu.fit.batyrkhanov.schoolsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}