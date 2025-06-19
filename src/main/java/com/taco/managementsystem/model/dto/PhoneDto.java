package com.taco.managementsystem.model.dto;

import com.taco.managementsystem.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDto {
    private User user;
    private String phone;
}
