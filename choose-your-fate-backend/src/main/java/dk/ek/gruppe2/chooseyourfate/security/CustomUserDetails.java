package dk.ek.gruppe2.chooseyourfate.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import dk.ek.gruppe2.chooseyourfate.enums.DataSourceType;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Account;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Account accountSql;

    public CustomUserDetails(Account accountSql) {
        this.accountSql = accountSql;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (accountSql.getRole() == null) {
            throw new IllegalStateException("Account role is null for user: " + accountSql.getUsername());
        }
        return List.of(new SimpleGrantedAuthority(accountSql.getRole().name()));
    }

    public Collection<? extends GrantedAuthority> getAuthorities(DataSourceType dataSource) {
        return getAuthoritiesSQL();
    }

    private Collection<? extends GrantedAuthority> getAuthoritiesSQL() {
        if (accountSql.getRole() == null) {
            throw new IllegalStateException("Account role is null for user: " + accountSql.getUsername());
        }

        return List.of(new SimpleGrantedAuthority(accountSql.getRole().name()));
    }

    public String getPassword(DataSourceType dataSource) {
        return accountSql.getPassword();
    }

    public String getUsername(DataSourceType dataSource) {
        return accountSql.getUsername();
    }

    public String getId(DataSourceType dataSource) {
        return accountSql.getId().toString();
    }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public @Nullable String getPassword() {
        return accountSql.getPassword();
    }

    @Override
    public String getUsername() {
        return accountSql.getUsername();
    }
}