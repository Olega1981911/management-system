package com.taco.managementsystem.mappertest;

import com.taco.managementsystem.model.converter.Converter;
import com.taco.managementsystem.model.dto.AccountDto;
import com.taco.managementsystem.model.dto.TransferDto;
import com.taco.managementsystem.model.dto.UserDto;
import com.taco.managementsystem.model.entity.Account;
import com.taco.managementsystem.model.entity.Transfer;

import com.taco.managementsystem.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Set;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ConverterTest {
    private ModelMapper modelMapper;
    private PasswordEncoder passwordEncoder;
    private Converter converter;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        passwordEncoder = mock(PasswordEncoder.class);
        converter = new Converter(modelMapper, passwordEncoder);
    }

    @Test
    void toTransferDtoShouldMapCorrectly() {
        User fromUser = new User();
        fromUser.setId(1L);
        User toUser = new User();
        toUser.setId(2L);

        Transfer transfer = new Transfer();
        transfer.setFromUser(fromUser);
        transfer.setToUser(toUser);
        transfer.setAmount(BigDecimal.valueOf(100));

        TransferDto dto = converter.toTransferDto(transfer);

        assertThat(dto).isNotNull();
        assertThat(dto.getFromUserId()).isEqualTo(1L);
        assertThat(dto.getToUserId()).isEqualTo(2L);
        assertThat(dto.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void accountToEntityShouldMapAndSetUser() {
        AccountDto dto = new AccountDto();
        dto.setInitialDeposit(BigDecimal.valueOf(500));
        User user = new User();
        user.setId(10L);

        Account account = converter.accountToEntity(dto, user);

        assertThat(account).isNotNull();
        assertThat(account.getUser()).isEqualTo(user);
        assertThat(account.getInitialDeposit()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void toEntityUserShouldMapUserAndEncodePassword() {
        UserDto dto = new UserDto();
        dto.setPassword("rawPassword");
        dto.setEmails(Set.of("test@example.com"));
        dto.setPhoneNumbers(Set.of("123456789"));
        dto.setBalance(BigDecimal.valueOf(1000));

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        User user = converter.toEntityUser(dto);

        assertThat(user).isNotNull();
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        assertThat(user.getEmails()).extracting("email").containsExactly("test@example.com");
        assertThat(user.getPhones()).extracting("phone").containsExactly("123456789");
        assertThat(user.getAccount()).isNotNull();
        assertThat(user.getAccount().getUser()).isEqualTo(user);
        assertThat(user.getAccount().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        // Проверяем, что initialDeposit установлен в то же значение, что и баланс
        assertThat(user.getAccount().getInitialDeposit()).isEqualByComparingTo(BigDecimal.valueOf(1000));

        verify(passwordEncoder).encode("rawPassword");
    }

}
