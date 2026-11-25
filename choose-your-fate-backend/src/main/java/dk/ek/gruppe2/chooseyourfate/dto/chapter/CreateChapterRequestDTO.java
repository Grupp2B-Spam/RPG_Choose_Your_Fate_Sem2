package dk.ek.gruppe2.chooseyourfate.dto.chapter;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;

public class CreateChapterRequestDTO {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Chapter toEntity() {
        Chapter chapter = new Chapter();
        chapter.setName(this.name);
        return chapter;
    }
}