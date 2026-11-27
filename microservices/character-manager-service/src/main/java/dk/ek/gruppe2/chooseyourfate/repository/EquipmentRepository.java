package dk.ek.gruppe2.chooseyourfate.repository;

import dk.ek.gruppe2.chooseyourfate.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Integer> {
}
