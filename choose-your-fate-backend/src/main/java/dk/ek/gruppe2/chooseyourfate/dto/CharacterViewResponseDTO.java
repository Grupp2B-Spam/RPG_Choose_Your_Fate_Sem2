package dk.ek.gruppe2.chooseyourfate.dto;

public class CharacterViewResponseDTO {

        Integer characterId;
        String characterName;
        Integer chapterId;
        String chapterName;
        Integer raceDetailsId;
        String raceName;
        CharacterStatsDTO stats;

        public CharacterViewResponseDTO(Integer characterId, String characterName, Integer chapterId,
                        String chapterName, Integer raceDetailsId, String raceName, CharacterStatsDTO stats) {
                this.characterId = characterId;
                this.characterName = characterName;
                this.chapterId = chapterId;
                this.chapterName = chapterName;
                this.raceDetailsId = raceDetailsId;
                this.raceName = raceName;
                this.stats = stats;
        }
        public Integer getCharacterId() {
                return characterId;
        }
        public void setCharacterId(Integer characterId) {
                this.characterId = characterId;
        }
        public String getCharacterName() {
                return characterName;
        }
        public void setCharacterName(String characterName) {
                this.characterName = characterName;
        }
        public Integer getChapterId() {
                return chapterId;
        }
        public void setChapterId(Integer chapterId) {
                this.chapterId = chapterId;
        }
        public String getChapterName() {
                return chapterName;
        }
        public void setChapterName(String chapterName) {
                this.chapterName = chapterName;
        }
        public Integer getRaceDetailsId() {
                return raceDetailsId;
        }
        public void setRaceDetailsId(Integer raceDetailsId) {
                this.raceDetailsId = raceDetailsId;
        }
        public String getRaceName() {
                return raceName;
        }
        public void setRaceName(String raceName) {
                this.raceName = raceName;
        }
        public CharacterStatsDTO getStats() {
                return stats;
        }
        public void setStats(CharacterStatsDTO stats) {
                this.stats = stats;
        }
}
