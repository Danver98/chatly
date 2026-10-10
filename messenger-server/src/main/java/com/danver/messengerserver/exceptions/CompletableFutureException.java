package com.danver.messengerserver.exceptions;

import java.io.Serial;

public class CompletableFutureException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 1L;

    public CompletableFutureException(Exception ex) {
        super(ex);
    }
}
