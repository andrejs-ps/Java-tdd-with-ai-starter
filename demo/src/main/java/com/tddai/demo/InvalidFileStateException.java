package com.tddai.demo;

/** The whole file is unusable (as opposed to a single bad line, which is only reported). */
public class InvalidFileStateException extends Exception {

    public InvalidFileStateException(String message) {
        super(message);
    }

    public InvalidFileStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
