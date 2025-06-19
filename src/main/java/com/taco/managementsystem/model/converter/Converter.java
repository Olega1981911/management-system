package com.taco.managementsystem.model.converter;

import com.taco.managementsystem.model.dto.*;
import com.taco.managementsystem.model.entity.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class Converter {
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;


    public TransferDto toTransferDto(Transfer transfer) {
        return TransferDto.builder()
                .fromUserId(transfer.getFromUser().getId())
                .toUserId(transfer.getToUser().getId())
                .amount(transfer.getAmount())
                .timestamp(transfer.getTimestamp())
                .status(transfer.getStatus().name()) // Конвертация enum в String
                .build();
    }

    public Account accountToEntity(AccountDto dto, User user) {
        Account account = modelMapper.map(dto, Account.class);
        account.setUser(user);
        account.setInitialDeposit(dto.getInitialDeposit());
        return account;
    }

    public Account balanceToEntity(BigDecimal balance, User user) {
        return Account.builder()
                .balance(balance)
                .user(user)
                .initialDeposit(balance)
                .build();
    }

    public User toEntityUser(UserDto dto) {
        User user = User.builder()
                .name(dto.getName())
                .dateOfBirth(dto.getDateOfBirth())
                .password(passwordEncoder.encode(dto.getPassword()))
                .build();

        user.setEmails(mapEmails(dto.getEmails(), user));
        user.setPhones(mapPhones(dto.getPhoneNumbers(), user));
        user.setAccount(balanceToEntity(dto.getBalance(), user));

        return user;
    }

    private Set<Email> mapEmails(Set<String> emails, User user) {
        return Optional.ofNullable(emails)
                .orElse(Collections.emptySet())
                .stream()
                .map(email -> Email.builder()
                        .email(email)
                        .user(user)
                        .build())
                .collect(Collectors.toSet());
    }

    private Set<Phone> mapPhones(Set<String> phones, User user) {
        return Optional.ofNullable(phones)
                .orElse(Collections.emptySet())
                .stream()
                .map(phone -> Phone.builder()
                        .phone(phone)
                        .user(user)
                        .build())
                .collect(Collectors.toSet());
    }

    public UserDto userToDto(User user) {
        return UserDto.builder()
                .name(user.getName())
                .dateOfBirth(user.getDateOfBirth())
                .emails(user.getEmails().stream()
                        .map(Email::getEmail)
                        .collect(Collectors.toSet()))
                .phoneNumbers(user.getPhones().stream()
                        .map(Phone::getPhone)
                        .collect(Collectors.toSet()))
                .balance(user.getAccount().getBalance())
                .build();
    }
}
