package dk.ek.gruppe2.chooseyourfate.unit.availability.replication;

import dk.ek.gruppe2.chooseyourfate.availability.replication.WriteGate;
import dk.ek.gruppe2.chooseyourfate.availability.replication.events.account.AccountCreatedEvent;
import dk.ek.gruppe2.chooseyourfate.availability.replication.events.account.AccountDeletedEvent;
import dk.ek.gruppe2.chooseyourfate.availability.replication.events.account.AccountUpdatedEvent;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import dk.ek.gruppe2.chooseyourfate.availability.routing.InvalidDatabaseStateTransitionException;
import dk.ek.gruppe2.chooseyourfate.dto.CreateAccountRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.UpdateAccountRequestDTO;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Account;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.AccountRepository;
import dk.ek.gruppe2.chooseyourfate.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceEventTest {

    private AccountRepository accountRepository;
    private WriteGate writeGate;
    private ApplicationEventPublisher events;
    private PasswordEncoder passwordEncoder;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        writeGate = mock(WriteGate.class);
        events = mock(ApplicationEventPublisher.class);
        passwordEncoder = mock(PasswordEncoder.class);

        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(accountRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(accountRepository.findByEmail(any())).thenReturn(Optional.empty());

        accountService = new AccountService(passwordEncoder, writeGate, events, accountRepository);
    }

    // ---------- PRIMARY partition: events SHOULD publish ----------

    @Test
    void createOnPrimaryShouldPublishAccountCreatedEvent() {
        // Arrange
        when(writeGate.getTargetForWrite()).thenReturn(DatabaseRole.PRIMARY);
        Account savedAccount = sampleAccount();
        when(accountRepository.saveAndFlush(any(Account.class))).thenReturn(savedAccount);

        // Act
        accountService.createAccount(sampleCreateRequest());

        // Assert
        verify(events).publishEvent(any(AccountCreatedEvent.class));
    }

    @Test
    void updateOnPrimaryShouldPublishAccountUpdatedEvent() {
        // Arrange
        when(writeGate.getTargetForWrite()).thenReturn(DatabaseRole.PRIMARY);
        Account existing = sampleAccount();
        when(accountRepository.findById(1)).thenReturn(Optional.of(existing));
        when(accountRepository.saveAndFlush(any(Account.class))).thenReturn(existing);

        // Act
        accountService.updateAccount(1, sampleUpdateRequest());

        // Assert
        verify(events).publishEvent(any(AccountUpdatedEvent.class));
    }

    @Test
    void deleteOnPrimaryShouldPublishAccountDeletedEvent() {
        // Arrange
        when(writeGate.getTargetForWrite()).thenReturn(DatabaseRole.PRIMARY);
        when(accountRepository.existsById(1)).thenReturn(true);

        // Act
        accountService.deleteAccount(1);

        // Assert
        verify(events).publishEvent(any(AccountDeletedEvent.class));
    }

    // ---------- SECONDARY partition: events SHOULD NOT publish ----------

    @Test
    void createOnSecondaryShouldNotPublishEvent() {
        // Arrange
        when(writeGate.getTargetForWrite()).thenReturn(DatabaseRole.SECONDARY);
        when(accountRepository.saveAndFlush(any(Account.class))).thenReturn(sampleAccount());

        // Act
        accountService.createAccount(sampleCreateRequest());

        // Assert
        verify(events, never()).publishEvent(any());
    }

    @Test
    void updateOnSecondaryShouldNotPublishEvent() {
        // Arrange
        when(writeGate.getTargetForWrite()).thenReturn(DatabaseRole.SECONDARY);
        Account existing = sampleAccount();
        when(accountRepository.findById(1)).thenReturn(Optional.of(existing));
        when(accountRepository.saveAndFlush(any(Account.class))).thenReturn(existing);

        // Act
        accountService.updateAccount(1, sampleUpdateRequest());

        // Assert
        verify(events, never()).publishEvent(any());
    }

    @Test
    void deleteOnSecondaryShouldNotPublishEvent() {
        // Arrange
        when(writeGate.getTargetForWrite()).thenReturn(DatabaseRole.SECONDARY);
        when(accountRepository.existsById(1)).thenReturn(true);

        // Act
        accountService.deleteAccount(1);

        // Assert
        verify(events, never()).publishEvent(any());
    }

    // ---------- Invalid partition: gate throws → nothing happens ----------

    @Test
    void writeBlockedByGateShouldNotTouchRepoOrEvents() {
        // Arrange
        when(writeGate.getTargetForWrite())
                .thenThrow(new InvalidDatabaseStateTransitionException("blocked"));

        // Act / Assert
        assertThrows(InvalidDatabaseStateTransitionException.class,
                () -> accountService.createAccount(sampleCreateRequest()));

        // Assert
        verify(accountRepository, never()).saveAndFlush(any(Account.class));
        verify(events, never()).publishEvent(any());
    }

    // ---------- Helpers ----------

    private CreateAccountRequestDTO sampleCreateRequest() {
        CreateAccountRequestDTO r = new CreateAccountRequestDTO();
        r.setUsername("player");
        r.setEmail("p@test.dk");
        r.setPassword("pw");
        return r;
    }

    private UpdateAccountRequestDTO sampleUpdateRequest() {
        UpdateAccountRequestDTO r = new UpdateAccountRequestDTO();
        r.setUsername("updated");
        return r;
    }

    private Account sampleAccount() {
        Account a = new Account();
        a.setId(1);
        a.setUsername("player");
        a.setEmail("p@test.dk");
        a.setCharacterLimit(3);
        a.setPassword("hashed");
        return a;
    }
}
