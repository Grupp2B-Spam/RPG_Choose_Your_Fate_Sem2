package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Account;

public class AccountResponseDTO {

    private Integer id;
    private String username;
    private Integer characterLimit;
    private String email;

    public AccountResponseDTO() {
    }

    public AccountResponseDTO(Integer id, String username, Integer characterLimit, String email) {
        this.id = id;
        this.username = username;
        this.characterLimit = characterLimit;
        this.email = email;
    }

    public AccountResponseDTO(Account account) {
        this.id = account.getId();
        this.username = account.getUsername();
        this.characterLimit = account.getCharacterLimit();
        this.email = account.getEmail();
    }

    public AccountResponseDTO toDTO(Account account) {
        return new AccountResponseDTO(
                account.getId(),
                account.getUsername(),
                account.getCharacterLimit(),
                account.getEmail()
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getCharacterLimit() {
        return characterLimit;
    }

    public void setCharacterLimit(Integer characterLimit) {
        this.characterLimit = characterLimit;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}