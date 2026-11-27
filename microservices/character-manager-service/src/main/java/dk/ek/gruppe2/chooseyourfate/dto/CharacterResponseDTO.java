package dk.ek.gruppe2.chooseyourfate.dto;

public class CharacterResponseDTO {

    private Integer id;
    private Integer accountId;
    private Integer chapterId;
    private Integer sceneId;
    private Integer raceDetailsId;
    private String name;
    private String flag;

    public CharacterResponseDTO() {
    }

    public CharacterResponseDTO(
            Integer id,
            Integer accountId,
            Integer chapterId,
            Integer sceneId,
            Integer raceDetailsId,
            String name,
            String flag
    ) {
        this.id = id;
        this.accountId = accountId;
        this.chapterId = chapterId;
        this.sceneId = sceneId;
        this.raceDetailsId = raceDetailsId;
        this.name = name;
        this.flag = flag;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getSceneId() {
        return sceneId;
    }

    public void setSceneId(Integer sceneId) {
        this.sceneId = sceneId;
    }

    public Integer getRaceDetailsId() {
        return raceDetailsId;
    }

    public void setRaceDetailsId(Integer raceDetailsId) {
        this.raceDetailsId = raceDetailsId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }
}