package dk.ek.gruppe2.chooseyourfate.controller;

import java.util.List;

import dk.ek.gruppe2.chooseyourfate.service.ChoiceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ek.gruppe2.chooseyourfate.dto.choice.ChoiceResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.choice.CreateChoiceRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.choice.UpdateChoiceRequestDTO;


/**
 * MIGRATED to StoryContentManager: {@code choice} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>A choice is part of the scene aggregate there and is never published on its own, so
 * it is edited through its scene. Two columns have no counterpart here:
 * {@code beat}, the authored sentence copied into a player's history, and the typed
 * effects that replaced {@code consequence}, {@code target_id} and
 * {@code value_int}.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/scenes/{id}/choices} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@RestController
@RequestMapping("/choose-your-fate/choices")
public class ChoiceController {

    private final ChoiceService choiceService;

    public ChoiceController(ChoiceService choiceService) {
        this.choiceService = choiceService;
    }

    @GetMapping
    public List<ChoiceResponseDTO> getAllChoices() {
        return choiceService.getAllChoices();
    }

    @GetMapping("/{id}")
    public ChoiceResponseDTO getChoiceById(@PathVariable Integer id
    ) {
        return choiceService.getChoiceById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ChoiceResponseDTO createChoice(@RequestBody CreateChoiceRequestDTO request
    ) {
        return choiceService.createChoice(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ChoiceResponseDTO updateChoice(
            @PathVariable Integer id,
            @RequestBody UpdateChoiceRequestDTO request
    ) {
        return choiceService.updateChoice(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteChoice(@PathVariable Integer id
    ) {
        choiceService.deleteChoice(id);
    }
}
