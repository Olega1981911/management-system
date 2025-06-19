package com.taco.managementsystem.util;

import com.taco.managementsystem.config.InterestProperties;
import com.taco.managementsystem.model.entity.Account;
import com.taco.managementsystem.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Interest {
    private final InterestProperties properties;
    private final AccountRepository accountRepository;
    private final CacheManager cacheManager;

    @Scheduled(fixedRateString = "${bank.interest.schedule-ms}")
    public void interest() {
        if (!properties.isEnabled()) {
            return;
        }
        List<Account> accounts = accountRepository.findAccountsEligibleForInterest(
                properties.getMaxMultiplier()
        );
        accounts.forEach(acc -> {
            BigDecimal newBalance = acc.getBalance().multiply(properties.getRate());
            BigDecimal maxAllowed = acc.getInitialDeposit().multiply(properties.getMaxMultiplier());
            acc.setBalance(newBalance.min(maxAllowed));
        });

        accountRepository.saveAll(accounts);
        invalidateBalanceCache(accounts);
    }

    private void invalidateBalanceCache(List<Account> accounts) {
        if (cacheManager == null) {
            log.warn("CacheManager is not configured, cannot invalidate cache");
            return;
        }
        var cache = cacheManager.getCache("accountBalance");
        accounts.forEach(acc -> {
            Long userId = acc.getUser().getId();
            cache.evict(userId);
            log.debug("Invalidated Redis cache 'accountBalance' for userId={}", userId);
        });
    }
}
