package dk.ek.gruppe2.recapmanager.storypath;

import java.util.List;

public interface JourneyHistoryClient {
    /** Complete, ordered snapshot of entries newer than the exclusive saved checkpoint. */
    List<JourneyChoice> fetchAfter(int characterId, long afterSequence);
}
