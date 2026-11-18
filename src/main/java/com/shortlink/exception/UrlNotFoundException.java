package com.shortlink.exception;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String code) {
        super("No short link found for code: " + code);
    }
}
