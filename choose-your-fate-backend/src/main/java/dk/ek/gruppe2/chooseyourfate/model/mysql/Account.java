package dk.ek.gruppe2.chooseyourfate.model.mysql;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.ek.gruppe2.chooseyourfate.enums.Role;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "character_limit", nullable = false)
    private Integer characterLimit;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "password",nullable = false, length = 100)
    private String password;

    @OneToMany(mappedBy = "account")
    @JsonIgnore
    private List<CharacterAvatar> characters = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public Account() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getCharacterLimit() { return characterLimit; }
    public void setCharacterLimit(Integer characterLimit) { this.characterLimit = characterLimit; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<CharacterAvatar> getCharacters() { return characters; }
    public void setCharacters(List<CharacterAvatar> characters) { this.characters = characters; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
