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

/**
 * MIGRATED to StoryContentManager: {@code chapter} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is one of the ten tables the content/state split assigned to that service. A
 * chapter is not published as an event of its own: it reaches consumers as
 * {@code chapterId} inside a scene payload and as {@code startingChapterId} inside a
 * race payload.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/chapters} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
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
