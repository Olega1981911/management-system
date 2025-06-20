package com.taco.managementsystem.testservice;

import com.taco.managementsystem.exeption.AccountNotFoundException;
import com.taco.managementsystem.exeption.InsufficientFundsException;
import com.taco.managementsystem.exeption.TransferException;
import com.taco.managementsystem.model.entity.Account;
import com.taco.managementsystem.model.entity.User;
import com.taco.managementsystem.redis.RedisLockManager;
import com.taco.managementsystem.repository.AccountRepository;
import com.taco.managementsystem.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RedisLockManager redisLockManager;

    @InjectMocks
    private AccountService accountService;

    private Account fromAccount;
    private Account toAccount;
    private final Long fromUserId = 1L;
    private final Long toUserId = 2L;
    private final BigDecimal amount = new BigDecimal("100.00");
    private final BigDecimal largeAmount = new BigDecimal("600.00");

    @BeforeEach
    void setUp() {
        User fromUser = new User();
        fromUser.setId(fromUserId);

        User toUser = new User();
        toUser.setId(toUserId);

        fromAccount = Account.builder()
                .id(1L)
                .user(fromUser)
                .balance(new BigDecimal("500.00"))
                .initialDeposit(new BigDecimal("500.00"))
                .build();

        toAccount = Account.builder()
                .id(2L)
                .user(toUser)
                .balance(new BigDecimal("200.00"))
                .initialDeposit(new BigDecimal("200.00"))
                .build();
    }

    @Test
    void transferMoneySuccessfulTransfer() {
        when(redisLockManager.acquireLock(anyString(), anyString(), any()))
                .thenReturn(true);

        when(accountRepository.findByUserIdWithLock(fromUserId))
                .thenReturn(Optional.of(fromAccount));

        when(accountRepository.findByUserIdWithLock(toUserId))
                .thenReturn(Optional.of(toAccount));

        accountService.transferMoney(fromUserId, toUserId, amount);


        assertEquals(new BigDecimal("400.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("300.00"), toAccount.getBalance());

        verify(redisLockManager, times(1)).acquireLock(eq("transfer-lock:1"), anyString(), any(Duration.class));
        verify(redisLockManager, times(1)).acquireLock(eq("transfer-lock:2"), anyString(), any(Duration.class));
        verify(redisLockManager, times(1)).releaseLock(eq("transfer-lock:1"), anyString());
        verify(redisLockManager, times(1)).releaseLock(eq("transfer-lock:2"), anyString());
    }

    @Test
    void transferMoneyInsufficientFunds() {

        when(redisLockManager.acquireLock(anyString(), anyString(), any()))
                .thenReturn(true);
        when(accountRepository.findByUserIdWithLock(fromUserId))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByUserIdWithLock(toUserId))
                .thenReturn(Optional.of(toAccount));

        assertThrows(InsufficientFundsException.class, () -> {
            accountService.transferMoney(fromUserId, toUserId, largeAmount);
        });
    }

    @Test
    void transferMoneyAccountNotFound() {
        when(redisLockManager.acquireLock(anyString(), anyString(), any()))
                .thenReturn(true);

        when(accountRepository.findByUserIdWithLock(fromUserId))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.transferMoney(fromUserId, toUserId, amount);
        });
    }

    @Test
    void transferMoneyLockAcquisitionFailed() {

        when(redisLockManager.acquireLock(anyString(), anyString(), any()))
                .thenReturn(false);

        assertThrows(TransferException.class, () -> {
            accountService.transferMoney(fromUserId, toUserId, amount);
        });
    }

    @Test
    void transferMoneyTransferToSelf() {
        assertThrows(TransferException.class, () -> {
            accountService.transferMoney(fromUserId, fromUserId, amount);
        });
    }

    @Test
    void transferMoneyNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.transferMoney(fromUserId, toUserId, new BigDecimal("-100"));
        });
    }

    @Test
    void transferMoneyZeroAmount() {

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.transferMoney(fromUserId, toUserId, BigDecimal.ZERO);
        });
    }

    @Test
    void transferMoneyLockReleaseCalledAlways() {

        when(redisLockManager.acquireLock(anyString(), anyString(), any()))
                .thenReturn(true);

        when(accountRepository.findByUserIdWithLock(fromUserId))
                .thenReturn(Optional.of(fromAccount));

        when(accountRepository.findByUserIdWithLock(toUserId))
                .thenReturn(Optional.of(toAccount));


        accountService.transferMoney(fromUserId, toUserId, amount);

        verify(redisLockManager, times(1)).releaseLock(eq("transfer-lock:1"), anyString());
        verify(redisLockManager, times(1)).releaseLock(eq("transfer-lock:2"), anyString());
    }

    @Test
    void transferMoneyLockReleaseCalledEvenOnException() {

        when(redisLockManager.acquireLock(anyString(), anyString(), any()))
                .thenReturn(true);

        Account lowBalanceAccount = Account.builder()
                .id(3L)
                .user(User.builder().id(fromUserId).build())
                .balance(new BigDecimal("50.00"))
                .initialDeposit(new BigDecimal("500.00"))
                .build();

        when(accountRepository.findByUserIdWithLock(fromUserId))
                .thenReturn(Optional.of(lowBalanceAccount));

        when(accountRepository.findByUserIdWithLock(toUserId))
                .thenReturn(Optional.of(toAccount));

        assertThrows(InsufficientFundsException.class, () -> {
            accountService.transferMoney(fromUserId, toUserId, amount);
        });

        verify(redisLockManager, times(1)).releaseLock(eq("transfer-lock:1"), anyString());
        verify(redisLockManager, times(1)).releaseLock(eq("transfer-lock:2"), anyString());
    }


}
