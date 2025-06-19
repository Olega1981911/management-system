package com.taco.managementsystem.controller;

import com.taco.managementsystem.model.dto.TransferDto;
import com.taco.managementsystem.model.dto.TransferRequestDto;
import com.taco.managementsystem.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "API для управления денежными переводами")
public class TransferController {
    private final TransferService transferService;

    @Operation(
            summary = "Создать перевод",
            description = "Инициирует перевод денег от одного пользователя другому",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Перевод успешно создан"),
                    @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    @PostMapping("/{fromUserId}")
    public ResponseEntity<String> createTransfer(
            @Parameter(description = "ID пользователя-отправителя", required = true)
            @PathVariable Long fromUserId, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные перевода",
            required = true,
            content = @Content(schema = @Schema(implementation = TransferRequestDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody TransferRequestDto transferRequestDto
    ) {
        transferService.createTransfer(
                fromUserId,
                transferRequestDto.getToUserId(),
                transferRequestDto.getAmount()
        );
        return ResponseEntity.ok("Transfer created");
    }


    @Operation(
            summary = "Получить переводы пользователя",
            description = "Возвращает страницу переводов, где пользователь является отправителем или получателем",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Страница переводов",
                            content = @Content(schema = @Schema(implementation = TransferDto.class))),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TransferDto>> getUserTransfers(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Long userId,
            @Parameter(hidden = true) Pageable pageable
    ) {
        Page<TransferDto> transfers = transferService.getUserTransfers(userId, pageable);
        return ResponseEntity.ok(transfers);
    }
}
