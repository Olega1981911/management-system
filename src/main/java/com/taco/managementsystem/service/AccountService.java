package com.taco.managementsystem.service;

import com.taco.managementsystem.config.InterestProperties;
import com.taco.managementsystem.exeption.AccountNotFoundException;
import com.taco.managementsystem.exeption.InsufficientFundsException;
import com.taco.managementsystem.exeption.TransferException;
import com.taco.managementsystem.model.entity.Account;
import com.taco.managementsystem.model.entity.User;
import com.taco.managementsystem.model.records.LockPair;
import com.taco.managementsystem.redis.RedisLockManager;
import com.taco.managementsystem.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;
    private final RedisLockManager redisLockManager;
    private final InterestProperties interestProperties;

    @Transactional
    public Account createAccount(User user, BigDecimal initialDeposit) {
        if (initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit must be greater than zero");
        }

        Account account = Account.builder()
                .user(user)
                .balance(initialDeposit)
                .initialDeposit(initialDeposit)
                .build();

        return accountRepository.save(account);
    }

    @Transactional
    public void applyInterest() {
        log.info("Starting interest application process");

        List<Account> accounts = accountRepository.findAccountsEligibleForInterest(
                interestProperties.getMaxMultiplier()
        );

        log.debug("Found {} accounts eligible for interest", accounts.size());

        for (Account account : accounts) {
            BigDecimal newBalance = account.getBalance().multiply(interestProperties.getRate());
            BigDecimal maxAllowed = account.getInitialDeposit().multiply(interestProperties.getMaxMultiplier());

            if (newBalance.compareTo(maxAllowed) > 0) {
                newBalance = maxAllowed;
            }

            if (newBalance.compareTo(account.getBalance()) != 0) {
                account.setBalance(newBalance);
                log.debug("Applied interest to account {}: new balance {}", account.getId(), newBalance);
            }
        }

        log.info("Interest application process completed");
    }


    @Transactional
    public void transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
        log.info("Initiating transfer from {} to {} of amount {}", fromUserId, toUserId, amount);

        validateTransferRequest(fromUserId, toUserId, amount);
        LockPair lockPair = createLockPair(fromUserId, toUserId);

        try {
            acquireLocks(lockPair);
            performTransfer(fromUserId, toUserId, amount);
            log.info("Successfully transferred {} from user {} to user {}", amount, fromUserId, toUserId);
        } finally {
            releaseLocks(lockPair);
        }
    }

    private void validateTransferRequest(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("You can't transfer more than one user");
        }
    }

    private LockPair createLockPair(Long fromUserId, Long toUserId) {
        var firstUserId = Math.min(fromUserId, toUserId);
        var secondUserId = Math.max(fromUserId, toUserId);
        return new LockPair(
                "transfer - lock:" + firstUserId,
                "transfer-lock:" + secondUserId,
                UUID.randomUUID().toString());
    }

    private void acquireLocks(LockPair lockPair) {
        log.debug("Attempting to acquire lock for account {}", lockPair.firstLockKey());
        boolean firstLockAcquired = redisLockManager.acquireLock(
                lockPair.firstLockKey(), lockPair.lockId(), Duration.ofSeconds(10)
        );
        if (!firstLockAcquired) {
            throw new InsufficientFundsException("Insufficient funds to acquire lock" + lockPair.firstLockKey());
        }
        log.debug("Acquired lock for account {}", lockPair.firstLockKey());

        log.debug("Attempting to acquire lock for account {}", lockPair.secondLockKey());
        boolean secondLockAcquired = redisLockManager.acquireLock(
                lockPair.secondLockKey(), lockPair.lockId(), Duration.ofSeconds(10)
        );

        if (!secondLockAcquired) {
            throw new TransferException("Could not acquire lock for receiver account: " + lockPair.secondLockKey());
        }
        log.debug("Acquired lock for account {}", lockPair.secondLockKey());
    }

    private void performTransfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        Account fromAccount = getAccountWithLock(fromUserId, "Sender");
        Account toAccount = getAccountWithLock(toUserId, "Receiver");
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account: " + fromAccount.getId() +
                                                 ". Current balance: " + fromAccount.getBalance() + ", required: " + amount);
        }
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
    }

    private void releaseLocks(LockPair lockPair) {
        log.debug("Releasing locks for accounts {} and {}", lockPair.firstLockKey(), lockPair.secondLockKey());
        redisLockManager.releaseLock(lockPair.secondLockKey(), lockPair.lockId());
        redisLockManager.releaseLock(lockPair.firstLockKey(), lockPair.lockId());
    }

    private Account getAccountWithLock(Long userId, String accountType) {
        Optional<Account> accountOpt = accountRepository.findByUserIdWithLock(userId);
        return accountOpt.orElseThrow(() ->
                new AccountNotFoundException(accountType + " account not found for user: " + userId));
    }

    @Transactional(readOnly = true)
    public Account getAccountByUserId(Long userId) {
        Optional<Account> accountOpt = accountRepository.findByUserId(userId);
        return accountOpt.orElseThrow(() ->
                new AccountNotFoundException("Account not found for user: " + userId));
    }
}
