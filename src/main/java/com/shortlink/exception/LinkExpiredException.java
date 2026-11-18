package com.shortlink.exception;

public class LinkExpiredException extends RuntimeException {
    public LinkExpiredException(String code) {
        super("This short link has expired: " + code);
    }
}
