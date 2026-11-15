package dk.ek.gruppe2.chooseyourfate.dto;

public class CharacterStatsDTO {
        Integer intelligence;
        Integer charisma;
        Integer fashion;
        public CharacterStatsDTO(Integer intelligence, Integer charisma, Integer fashion) {
                this.intelligence = intelligence;
                this.charisma = charisma;
                this.fashion = fashion;
        }
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