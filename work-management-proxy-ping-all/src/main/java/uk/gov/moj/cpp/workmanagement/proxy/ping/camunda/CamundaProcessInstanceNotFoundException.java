package uk.gov.moj.cpp.workmanagement.proxy.ping.camunda;

public class CamundaProcessInstanceNotFoundException extends RuntimeException {

    public CamundaProcessInstanceNotFoundException(final String message) {
        super(message);
    }
}
