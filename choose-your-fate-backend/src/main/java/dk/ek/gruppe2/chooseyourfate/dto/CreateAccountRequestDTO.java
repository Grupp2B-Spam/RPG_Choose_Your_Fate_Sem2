package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.enums.Role;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Account;

public class CreateAccountRequestDTO {

    private String username;
    private String email;
    private String password;

    public CreateAccountRequestDTO() {
    }

    public Account toEntity() {
        Account account = new Account();
        account.setUsername(this.username);
        account.setEmail(this.email);
        account.setRole(Role.ROLE_USER);
        return account;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}