package com.xuvigan.hiveworks.storage;

/**
 * Unchecked storage failure.
 */
public final class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
