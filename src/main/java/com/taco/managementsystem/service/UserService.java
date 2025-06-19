package com.taco.managementsystem.service;

import com.taco.managementsystem.model.converter.Converter;
import com.taco.managementsystem.model.dto.UserDto;
import com.taco.managementsystem.model.entity.Account;
import com.taco.managementsystem.model.entity.Email;
import com.taco.managementsystem.model.entity.Phone;
import com.taco.managementsystem.model.entity.User;
import com.taco.managementsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final Converter converter;

    @Transactional
    public User createUser(UserDto dto) {
        User user = converter.toEntityUser(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        Account account = accountService.createAccount(user, dto.getBalance());
        user.setAccount(account);
        account.setUser(user);
        return userRepository.save(user);
    }

    @Cacheable(value = "users", key = "#userId")
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return converter.userToDto(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updateUserEmails(Long userId, Set<String> emails) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        for (String email : emails) {
            boolean exists = userRepository.existsByEmail(email);
            if (exists) {
                throw new RuntimeException("Email already exists");
            }
        }
        user.getEmails().clear();
        user.getEmails().addAll(emails.stream()
                .map(e -> Email.builder().email(e).user(user).build())
                .collect(Collectors.toSet()));
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updateUserPhones(Long userId, Set<String> newPhones) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        for (String phone : newPhones) {
            boolean exists = userRepository.existsByPhone(phone);
            if (exists) {
                throw new IllegalArgumentException("Phone " + phone + " is already in use");
            }
        }

        user.getPhones().clear();
        user.getPhones().addAll(newPhones.stream()
                .map(p -> Phone.builder().phone(p).user(user).build())
                .collect(Collectors.toSet()));

        userRepository.save(user);
    }

    public Page<UserDto> searchUsers(LocalDate dateOfBirth, String phone, String name, String email, Pageable pageable) {
        if (dateOfBirth != null) {
            return userRepository.findByDateOfBirthAfter(dateOfBirth, pageable)
                    .map(converter::userToDto);
        } else if (phone != null) {
            return userRepository.findByPhone(phone, pageable)
                    .map(converter::userToDto);
        } else if (name != null) {
            return userRepository.findByNameStartingWith(name, pageable)
                    .map(converter::userToDto);
        } else if (email != null) {
            return userRepository.findByEmail(email, pageable)
                    .map(converter::userToDto);
        } else {
            return userRepository.findAll(pageable)
                    .map(converter::userToDto);
        }
    }
}

