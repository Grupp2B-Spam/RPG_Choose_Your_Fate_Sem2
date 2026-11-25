package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.RaceDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceDetailsRepository extends JpaRepository<RaceDetails, Integer> {
}