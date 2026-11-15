package dk.ek.gruppe2.chooseyourfate.repository;

import dk.ek.gruppe2.chooseyourfate.model.RaceDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceDetailsRepository extends JpaRepository<RaceDetails, Integer> {
}