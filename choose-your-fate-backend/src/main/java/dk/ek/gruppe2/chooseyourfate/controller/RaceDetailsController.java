package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.RaceDetailsRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.RaceDetailsResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.RaceDetailsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/choose-your-fate/race-details")
public class RaceDetailsController {

    private final RaceDetailsService raceDetailsService;

    public RaceDetailsController(RaceDetailsService raceDetailsService) {
        this.raceDetailsService = raceDetailsService;
    }

    @GetMapping
    public List<RaceDetailsResponseDTO> getAllRaceDetails() {
        return raceDetailsService.getAllRaceDetails();
    }

    @GetMapping("/{id}")
    public RaceDetailsResponseDTO getRaceDetailsById(@PathVariable Integer id) {
        return raceDetailsService.getRaceDetailsById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public RaceDetailsResponseDTO createRaceDetails(@RequestBody RaceDetailsRequestDTO request) {
        return raceDetailsService.createRaceDetails(request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRaceDetails(@PathVariable Integer id) {
        raceDetailsService.deleteRaceDetails(id);
    }
}
