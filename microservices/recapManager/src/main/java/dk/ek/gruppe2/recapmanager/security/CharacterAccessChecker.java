package dk.ek.gruppe2.recapmanager.security;

/** Boundary for CharacterManager ownership retrieval. Currently backed by local fixtures only. */
@FunctionalInterface
public interface CharacterAccessChecker {
    boolean canAccess(Integer characterId, String subject);
}
