package dk.ek.gruppe2.chooseyourfate.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Account;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.AccountRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository repo;

    public CustomUserDetailsService(AccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public CustomUserDetails loadUserByUsername(String username) {
        Account acc = repo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("SQL Not found"));

        return new CustomUserDetails(acc);
    }
}
