package com.danver.messengerserver.exceptions;

import java.io.IOException;
import java.io.Serial;

public class StorageException extends IOException {
    @Serial
    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }
}
