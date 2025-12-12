package uk.gov.moj.cpp.workmanagement.proxy.ping.http;

public class ContextHttpPingException extends RuntimeException {

    public ContextHttpPingException(final String message) {
        super(message);
    }
    public ContextHttpPingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
