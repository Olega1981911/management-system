package com.taco.managementsystem.model.converter;

import com.taco.managementsystem.model.dto.*;
import com.taco.managementsystem.model.entity.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
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
        return modelMapper.map(transfer, TransferDto.class);
    }

    public Account accountToEntity(AccountDto dto, User user) {
        Account account = modelMapper.map(dto, Account.class);
        account.setUser(user);
        account.setBalance(dto.getInitialDeposit());
        return account;
    }

    public Account balanceToEntity(BigDecimal balance, User user) {
        Account account = new Account();
        account.setBalance(balance);
        account.setInitialDeposit(balance);
        account.setUser(user);
        return account;
    }

    public User toEntityUser(UserDto dto) {
        User user = modelMapper.map(dto, User.class);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
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
        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setEmails(user.getEmails().stream()
                .map(Email::getEmail)
                .collect(Collectors.toSet()));
        dto.setPhoneNumbers(user.getPhones().stream()
                .map(Phone::getPhone)
                .collect(Collectors.toSet()));
        dto.setBalance(user.getAccount().getBalance());
        return dto;
    }
}
