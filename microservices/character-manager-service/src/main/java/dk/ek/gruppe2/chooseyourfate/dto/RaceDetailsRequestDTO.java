package dk.ek.gruppe2.chooseyourfate.dto;

public class RaceDetailsRequestDTO {
    private String name;
    private Integer startingChapterId;

    public Integer getStartingChapterId() {
        return startingChapterId;
    }

    public void setStartingChapterId(Integer startingChapterId) {
        this.startingChapterId = startingChapterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
