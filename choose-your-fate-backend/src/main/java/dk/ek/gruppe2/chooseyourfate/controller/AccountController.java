package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.AccountResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CreateAccountRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.UpdateAccountRequestDTO;
import dk.ek.gruppe2.chooseyourfate.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/choose-your-fate/accounts")
public class AccountController {

    private final AccountService accountService;


    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccountResponseDTO> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponseDTO getAccountById(
            @PathVariable Integer id
    ) {
        return accountService.getAccountById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponseDTO createAccount(
            @RequestBody CreateAccountRequestDTO request
    ) {
        return accountService.createAccount(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @accountAuthorizationService.canModifyAccount(#id, authentication)")
    public AccountResponseDTO updateAccount(
            @PathVariable Integer id,
            @RequestBody UpdateAccountRequestDTO request
    ) {
        return accountService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAccount(
            @PathVariable Integer id
    ) {
        accountService.deleteAccount(id);
    }
}
