package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterAvatarRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("characterAuthorizationService")
public class CharacterAuthorizationService {

    private final CharacterAvatarRepository characterAvatarRepository;

    public CharacterAuthorizationService(CharacterAvatarRepository characterAvatarRepository) {
        this.characterAvatarRepository = characterAvatarRepository;
    }

    public boolean canAccessCharacter(Integer characterId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            return true;
        }

        return characterAvatarRepository.findById(characterId)
                .map(character -> character.getAccount().getUsername().equals(authentication.getName()))
                .orElse(false);
    }
}