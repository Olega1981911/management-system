package com.taco.managementsystem.exeption;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex, WebRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Account not found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex, WebRequest request) {
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Insufficient funds",
                ex.getMessage()
        );
    }

    @ExceptionHandler(TransferException.class)
    public ProblemDetail handleTransferException(TransferException ex, WebRequest request) {
        return createProblemDetail(
                HttpStatus.CONFLICT,
                "Transfer error",
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllExceptions(Exception ex, WebRequest request) {
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred"
        );
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setType(URI.create("https://api.taco.com/errors/" + status.value()));
        return problemDetail;
    }
}
