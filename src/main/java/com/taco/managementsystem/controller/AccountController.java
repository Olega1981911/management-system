package com.taco.managementsystem.controller;

import com.taco.managementsystem.model.entity.Account;
import com.taco.managementsystem.model.entity.User;
import com.taco.managementsystem.service.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
@Tag(name = "Аккаунт", description = "Действие с аккаунтом")
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/create")
    public ResponseEntity<Account> createAccount(@RequestParam Long userId,
                                                 @RequestParam BigDecimal initialDeposit) {
        User user = new User();
        user.setId(userId);
        Account account = accountService.createAccount(user, initialDeposit);
        return ResponseEntity.ok(account);
    }
    @GetMapping("/{userId}")
    public ResponseEntity<Account> getAccount(@PathVariable Long userId) {
        Account account = accountService.getAccountByUserId(userId);
        return ResponseEntity.ok(account);
    }
    @PostMapping("/transfer")
    public ResponseEntity<String> transferMoney(@RequestParam Long fromUserId,
                                                @RequestParam Long toUserId,
                                                @RequestParam BigDecimal amount) {
        accountService.transferMoney(fromUserId, toUserId, amount);
        return ResponseEntity.ok("Transfer successful");
    }
}
