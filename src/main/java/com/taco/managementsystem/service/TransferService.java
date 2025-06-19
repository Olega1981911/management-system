package com.taco.managementsystem.service;


import com.taco.managementsystem.exeption.AccountNotFoundException;
import com.taco.managementsystem.model.converter.Converter;
import com.taco.managementsystem.model.dto.TransferDto;
import com.taco.managementsystem.model.entity.Transfer;
import com.taco.managementsystem.model.entity.TransferStatus;
import com.taco.managementsystem.model.entity.User;
import com.taco.managementsystem.repository.TransferRepository;
import com.taco.managementsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {
    private final TransferRepository transferRepository;
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final Converter converter;

    @Transactional
    public void createTransfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (fromUserId == null || toUserId == null) {
            throw new AccountNotFoundException("User not found");
        }
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(()-> new AccountNotFoundException("User not found"));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(()-> new AccountNotFoundException("User not found"));
        Transfer transfer = Transfer.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .status(TransferStatus.PENDING)
                .build();

        transferRepository.save(transfer);
    }


    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processPendingTransfers() {
        List<Transfer> pending = transferRepository.findByStatus(TransferStatus.PENDING);
        pending.forEach(transfer -> {
            try {
                processTransfer(transfer.getId());
            } catch (Exception e) {
                log.error("Failed to process transfer {}", transfer.getId(), e);
            }
        });
    }

    @Transactional
    protected void processTransfer(Long id) {
        Transfer transfer = transferRepository.findByIdForUpdate(id)
                .orElseThrow(()-> new AccountNotFoundException("Transfer not found"));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            return;
        }
        try {
            accountService.transferMoney(
                    transfer.getFromUser().getId(),
                    transfer.getToUser().getId(),
                    transfer.getAmount()
            );
            transfer.setStatus(TransferStatus.SUCCESS);
            log.info("Transfer {} processed successfully", id);
        } catch (Exception e) {
            transfer.setStatus(TransferStatus.FAILED);
            log.error("Transfer {} failed: {}", id, e.getMessage());
        }
    }

    public Page<TransferDto> getUserTransfers(Long userId, Pageable pageable) {
        return transferRepository.findByUserId(userId, pageable)
                .map(converter::toTransferDto);
    }

}
