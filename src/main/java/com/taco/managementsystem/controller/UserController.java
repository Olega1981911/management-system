package com.taco.managementsystem.controller;

import com.taco.managementsystem.model.dto.UserDto;
import com.taco.managementsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("api/users")
@Tag(name = "User API", description = "Операции с пользователями")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "Получить пользователя по ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        var userDto = userService.getUserById(id);
        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "Поиск пользователей с фильтрацией и пагинацией")
    @GetMapping
    public ResponseEntity<Page<UserDto>> searchUsers(@Parameter(description = "Фильтр по дате рождения (больше чем)")
                                                     @RequestParam(required = false) LocalDate dateOfBirth,
                                                     @Parameter(description = "Фильтр по телефону (точное совпадение)")
                                                     @RequestParam(required = false) String phone,
                                                     @Parameter(description = "Фильтр по имени (начинается с)")
                                                     @RequestParam(required = false) String name,
                                                     @Parameter(description = "Фильтр по email (точное совпадение)")
                                                     @RequestParam(required = false) String email,
                                                     @ParameterObject Pageable pageable) {
        Page<UserDto> page = userService.searchUsers(dateOfBirth, phone, name, email, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Обновить email пользователя")
    @PutMapping("/{id}/emails")
    public ResponseEntity<Void> updateEmails(
            @PathVariable Long id,
            @RequestBody @Valid Set<String> emails) {
        userService.updateUserEmails(id, emails);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Обновить телефоны пользователя")
    @PutMapping("/{id}/phones")
    public ResponseEntity<Void> updatePhones(
            @PathVariable Long id,
            @RequestBody @Valid Set<String> phones
    ) {
        userService.updateUserPhones(id, phones);
        return ResponseEntity.noContent().build();
    }
}
