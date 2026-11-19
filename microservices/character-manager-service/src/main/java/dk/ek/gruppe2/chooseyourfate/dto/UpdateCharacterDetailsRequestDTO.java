package dk.ek.gruppe2.chooseyourfate.dto;

public class UpdateCharacterDetailsRequestDTO {

    private Integer intelligence;
    private Integer charisma;
    private Integer fashion;

    public Integer getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(Integer intelligence) {
        this.intelligence = intelligence;
    }

    public Integer getCharisma() {
        return charisma;
    }

    public void setCharisma(Integer charisma) {
        this.charisma = charisma;
    }

    public Integer getFashion() {
        return fashion;
    }

    public void setFashion(Integer fashion) {
        this.fashion = fashion;
    }
}
