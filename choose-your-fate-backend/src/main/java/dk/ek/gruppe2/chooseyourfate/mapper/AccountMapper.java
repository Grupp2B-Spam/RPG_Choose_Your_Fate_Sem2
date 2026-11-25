package dk.ek.gruppe2.chooseyourfate.mapper;

import dk.ek.gruppe2.chooseyourfate.dto.AccountResponseDTO;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponseDTO toDTO(Account account) {
        return new AccountResponseDTO(
                account.getId(),
                account.getUsername(),
                account.getCharacterLimit(),
                account.getEmail()
        );
    }
}