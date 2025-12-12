package uk.gov.moj.cpp.workmanagement.proxy.api.exception;

public class InputDataException extends RuntimeException {

    public InputDataException() {
        super();
    }

    public InputDataException(final String error) {
        super(error);
    }
}
