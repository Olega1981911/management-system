package com.taco.managementsystem.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taco.managementsystem.model.dto.TransferRequestDto;
import com.taco.managementsystem.model.entity.Account;
import com.taco.managementsystem.model.entity.Email;
import com.taco.managementsystem.model.entity.Phone;
import com.taco.managementsystem.model.entity.User;
import com.taco.managementsystem.repository.AccountRepository;
import com.taco.managementsystem.repository.UserRepository;
import com.taco.managementsystem.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class TransferControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    private Long fromUserId;
    private Long toUserId;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void setup() {
        accountRepository.deleteAll();
        userRepository.deleteAll();

        User fromUser = User.builder()
                .name("John Sender")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .password("password123")
                .build();

        // Создание Email объектов
        Email fromUserEmail = new Email();
        fromUserEmail.setEmail("john@example.com");
        fromUser.setEmails(Set.of(fromUserEmail));

        // Создание Phone объектов
        Phone fromUserPhone = new Phone();
        fromUserPhone.setPhone("+1234567890");
        fromUser.setPhones(Set.of(fromUserPhone));

        fromUser = userRepository.save(fromUser);
        fromUserId = fromUser.getId();

        // Создание счета для отправителя
        Account fromAccount = Account.builder()
                .user(fromUser)
                .balance(new BigDecimal("1000.00"))
                .initialDeposit(new BigDecimal("1000.00"))
                .build();
        accountRepository.save(fromAccount);

        // Создание пользователя-получателя
        User toUser = User.builder()
                .name("Jane Receiver")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .password("password456")
                .build();

        // Создание Email объектов
        Email toUserEmail = new Email();
        toUserEmail.setEmail("jane@example.com");
        toUser.setEmails(Set.of(toUserEmail));

        // Создание Phone объектов
        Phone toUserPhone = new Phone();
        toUserPhone.setPhone("+0987654321");
        toUser.setPhones(Set.of(toUserPhone));

        toUser = userRepository.save(toUser);
        toUserId = toUser.getId();

        // Создание счета для получателя
        Account toAccount = Account.builder()
                .user(toUser)
                .balance(new BigDecimal("500.00"))
                .initialDeposit(new BigDecimal("500.00"))
                .build();
        accountRepository.save(toAccount);
    }

    @Test
    void createTransferSuccess() throws Exception {
        TransferRequestDto request = new TransferRequestDto();
        request.setToUserId(toUserId);
        request.setAmount(new BigDecimal("100.00"));

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transfers/" + fromUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        Account senderAccount = accountRepository.findByUserId(fromUserId).orElseThrow();
        Account receiverAccount = accountRepository.findByUserId(toUserId).orElseThrow();

        assertEquals(new BigDecimal("900.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("600.00"), receiverAccount.getBalance());
    }

    @Test
    void createTransferInvalidRequest() throws Exception {
        TransferRequestDto request = new TransferRequestDto();
        request.setToUserId(toUserId);
        request.setAmount(new BigDecimal("-100.00"));

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transfers/" + fromUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}

