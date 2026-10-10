package com.danver.messengerserver.exceptions;

import java.io.Serial;

public class AppAuthenticationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public AppAuthenticationException(String message) {
        super(message);
    }
}
