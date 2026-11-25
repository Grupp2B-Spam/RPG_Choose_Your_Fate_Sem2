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
