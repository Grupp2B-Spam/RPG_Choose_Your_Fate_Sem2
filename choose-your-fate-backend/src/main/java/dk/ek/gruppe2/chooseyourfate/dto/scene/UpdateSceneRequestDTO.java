package dk.ek.gruppe2.chooseyourfate.dto.scene;

public class UpdateSceneRequestDTO {
    private String name;
    private Integer chapterId;
    private String mongoChapterId;

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMongoChapterId() {
        return mongoChapterId;
    }

    public void setMongoChapterId(String mongoChapterId) {
        this.mongoChapterId = mongoChapterId;
    }
}
