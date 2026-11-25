package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.availability.replication.WriteGate;
import dk.ek.gruppe2.chooseyourfate.availability.replication.events.account.AccountCreatedEvent;
import dk.ek.gruppe2.chooseyourfate.availability.replication.events.account.AccountDeletedEvent;
import dk.ek.gruppe2.chooseyourfate.availability.replication.events.account.AccountUpdatedEvent;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import dk.ek.gruppe2.chooseyourfate.dto.AccountResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CreateAccountRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.UpdateAccountRequestDTO;
import dk.ek.gruppe2.chooseyourfate.enums.Role;
import dk.ek.gruppe2.chooseyourfate.exception.DuplicateResourceException;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Account;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.AccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final WriteGate writeGate;

    public AccountService(
            PasswordEncoder passwordEncoder,
            WriteGate writeGate,
            ApplicationEventPublisher events,
            AccountRepository accountRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.events = events;
        this.writeGate = writeGate;
    }

    // reads
    public List<AccountResponseDTO> getAllAccounts() {
        return accountRepository.findAll().stream().map(AccountResponseDTO::new).toList();
    }

    public AccountResponseDTO getAccountById(Integer id) {

        Optional<Account> accountFound = accountRepository.findById(id);
        if(accountFound.isEmpty()) {
            throw new ResourceNotFoundException("No Account found with id " + id);
        }
        return new AccountResponseDTO(accountFound.get());
    }

    // writes
    @Transactional
    public AccountResponseDTO createAccount(CreateAccountRequestDTO createAccountRequest) {
        DatabaseRole target = writeGate.getTargetForWrite();

        ensureUniqueUsername(createAccountRequest.getUsername(), null);
        ensureUniqueEmail(createAccountRequest.getEmail(), null);

        Account accountToCreate = createAccountRequest.toEntity();
        accountToCreate.setCharacterLimit(3);
        accountToCreate.setPassword(passwordEncoder.encode(createAccountRequest.getPassword()));

        AccountResponseDTO savedAccount = new AccountResponseDTO(accountRepository.saveAndFlush(accountToCreate)) ;
        if(target == DatabaseRole.PRIMARY){
            events.publishEvent(new AccountCreatedEvent(createAccountPayload(savedAccount,createAccountRequest)));
        }
        return savedAccount;
    }

    @Transactional
    public AccountResponseDTO updateAccount(Integer id, UpdateAccountRequestDTO updateAccountRequest) {
        DatabaseRole target = writeGate.getTargetForWrite();
        Account accountToUpdate = getAccountEntity(id);

        if(updateAccountRequest.getUsername() != null && !updateAccountRequest.getUsername().equals(accountToUpdate.getUsername())) {
            ensureUniqueUsername(updateAccountRequest.getUsername(),id);
            accountToUpdate.setUsername(updateAccountRequest.getUsername());
        }
        if(updateAccountRequest.getEmail() != null && !updateAccountRequest.getEmail().equals(accountToUpdate.getEmail())) {
            ensureUniqueEmail(updateAccountRequest.getEmail(),id);
            accountToUpdate.setEmail(updateAccountRequest.getEmail());
        }
        if(updateAccountRequest.getPassword() != null && !updateAccountRequest.getPassword().equals(accountToUpdate.getPassword())) {
            accountToUpdate.setPassword(passwordEncoder.encode(updateAccountRequest.getPassword()));
        }
        if(updateAccountRequest.getCharacterLimit() != null) {
            accountToUpdate.setCharacterLimit(updateAccountRequest.getCharacterLimit());
        }

        AccountResponseDTO accountUpdated = new AccountResponseDTO(accountRepository.saveAndFlush(accountToUpdate));
        if(target == DatabaseRole.PRIMARY){
            events.publishEvent(new AccountUpdatedEvent(updateAccountPayload(accountUpdated,updateAccountRequest)));
        }
        return accountUpdated;
    }

    @Transactional
    public void deleteAccount(Integer id) {
        DatabaseRole target = writeGate.getTargetForWrite();
        if(!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("No Account found with id " + id);
        }
        accountRepository.deleteById(id);
        if(target == DatabaseRole.PRIMARY){
            events.publishEvent(new AccountDeletedEvent(Map.of("id",id)));
        }
    }

    @Transactional
    public AccountResponseDTO registerAccount(CreateAccountRequestDTO request) {
        return createAccount(request);
    }

    private Account getAccountEntity(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }

    private void ensureUniqueUsername(String username, Integer currentId) {
        accountRepository.findByUsername(username)
                .filter(account -> !account.getId().equals(currentId))
                .ifPresent(account -> {
                    throw new DuplicateResourceException("Username already exists");
                });
    }

    private void ensureUniqueEmail(String email, Integer currentId) {
        accountRepository.findByEmail(email)
                .filter(account -> !account.getId().equals(currentId))
                .ifPresent(account -> {
                    throw new DuplicateResourceException("Email already exists");
                });
    }

    private Map<String, Object> createAccountPayload (AccountResponseDTO account, CreateAccountRequestDTO request){
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", account.getId());
        payload.put("username", account.getUsername());
        payload.put("email", account.getEmail());
        payload.put("characterLimit", account.getCharacterLimit());
        payload.put("password", passwordEncoder.encode(request.getPassword()));
        payload.put("role", Role.ROLE_USER.name());
        return payload;
    }

    private Map<String, Object> updateAccountPayload (AccountResponseDTO account, UpdateAccountRequestDTO request){
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", account.getId());
        payload.put("username", account.getUsername());
        payload.put("email", account.getEmail());
        payload.put("characterLimit", account.getCharacterLimit());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            payload.put("password", passwordEncoder.encode(request.getPassword()));
        }
        return payload;
    }
}
