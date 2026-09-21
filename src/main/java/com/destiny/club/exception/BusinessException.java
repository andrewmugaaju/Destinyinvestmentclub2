package com.destiny.club.exception;

/** Thrown when a business rule is violated (e.g. an unbalanced deposit split, insufficient balance). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
