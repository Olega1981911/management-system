package com.taco.managementsystem.exeption;

public class InsufficientFundsException extends RuntimeException {
       public InsufficientFundsException(String message) {
        super(message);
    }


}
