package com.taco.managementsystem.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String name;
    private LocalDate dateOfBirth;
    private String password;
    private Set<String> emails;
    private Set<String> phoneNumbers;
    private BigDecimal balance;
}
