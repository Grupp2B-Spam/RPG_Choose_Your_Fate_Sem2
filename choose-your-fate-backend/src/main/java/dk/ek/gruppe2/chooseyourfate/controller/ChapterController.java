package dk.ek.gruppe2.chooseyourfate.controller;

import java.util.List;

import dk.ek.gruppe2.chooseyourfate.service.ChapterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ek.gruppe2.chooseyourfate.dto.chapter.ChapterResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.chapter.CreateChapterRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.chapter.UpdateChapterRequestDTO;

@RestController
@RequestMapping("/choose-your-fate/chapter")
public class ChapterController {
    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping
    public List<ChapterResponseDTO> getAllChapters(
    ) {
        return chapterService.getAllChapters();
    }

    @GetMapping("/{id}")
    public ChapterResponseDTO getChapterById(
            @PathVariable Integer id
    ) {
        return chapterService.getChapterById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ChapterResponseDTO createChapter(
            @RequestBody CreateChapterRequestDTO request
    ) {
        return chapterService.createChapter(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ChapterResponseDTO updateChapter(
            @PathVariable Integer id,
            @RequestBody UpdateChapterRequestDTO request
    ) {
        return chapterService.updateChapter(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteChapter(
            @PathVariable Integer id
    ) {
        chapterService.deleteChapter(id);
    }
}
