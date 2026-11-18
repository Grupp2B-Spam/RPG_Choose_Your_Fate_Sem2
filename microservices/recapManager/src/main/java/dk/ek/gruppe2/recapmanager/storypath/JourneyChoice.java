package dk.ek.gruppe2.recapmanager.storypath;

public record JourneyChoice(long sequence, int choiceId, String fromScene, String toScene,
                            String description, String consequence, int storyWeight) {
    public JourneyChoice {
        if (sequence <= 0 || choiceId <= 0 || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Journey choice requires a positive sequence/id and description");
        }
    }
}
