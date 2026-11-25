package dk.ek.gruppe2.chooseyourfate.dto.chapter;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;

public class ChapterResponseDTO {
    private Integer id;
    private String name;

    public ChapterResponseDTO() {
    }

    public ChapterResponseDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public ChapterResponseDTO(Chapter Chapter) {
        this.id = Chapter.getId();
        this.name = Chapter.getName();
    }

    public ChapterResponseDTO toDTO(Chapter Chapter) {
        return new ChapterResponseDTO(
                Chapter.getId(),
                Chapter.getName()
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
