package com.taco.managementsystem.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "bank.interest")
@Validated
@Data
public class InterestProperties {
    @DecimalMin("1.01")
    private BigDecimal rate = new BigDecimal("1.10");

    @DecimalMin("1.01")
    private BigDecimal maxMultiplier = new BigDecimal("2.07");

    @Min(1000)
    private long scheduleMs = 30000;

    private boolean enabled = true;
}
