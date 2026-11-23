package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.RaceDetailsRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.RaceDetailsResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.RaceDetailsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code race_details} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is called {@code race} there, and is published as
 * {@code race-definition.updated}. It went to StoryContentManager rather than
 * CharacterManager because {@code starting_chapter_id} is a mandatory reference into
 * story content: the other way round, CharacterManager would hold a foreign key into
 * another service's database. CharacterManager is expected to cache the event
 * instead, and {@code character_avatar.race_detail_id} becomes a plain integer.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/races} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
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
