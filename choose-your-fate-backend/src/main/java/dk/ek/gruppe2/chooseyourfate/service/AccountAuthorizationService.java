package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.repository.mysql.AccountRepository;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("accountAuthorizationService")
public class AccountAuthorizationService {

    private final AccountRepository accountRepository;

    public AccountAuthorizationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public boolean canModifyAccount(Integer accountId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            return true;
        }

        return accountRepository.findById(accountId)
                .map(account -> account.getUsername().equals(authentication.getName()))
                .orElse(false);
    }

    public boolean canModifyAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            return true;
        }

        Map<String, Object> extraInfo =  (Map<String, Object>) authentication.getDetails(); 

        Integer accountId = Integer.parseInt(extraInfo.get("sqlId").toString());

        return accountRepository.findById(accountId)
                .map(account -> account.getUsername().equals(authentication.getName()))
                .orElse(false);
    }
}
