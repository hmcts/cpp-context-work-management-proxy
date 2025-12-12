package uk.gov.moj.cpp.workmanagement.proxy.api.exception;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.WorkManagementResponseBuilder;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@ApplicationScoped
public class ExceptionProvider {

    private static final Logger LOGGER = getLogger(ExceptionProvider.class);

    @Inject
    private WorkManagementResponseBuilder responseBuilder;

    /**
     * Check status code of the response from Camunda and throw appropriate exception
     */
    @SuppressWarnings({"squid:S00112", "squid:S1192"})
    public void throwApplicationSpecificException(final Response response) {

        final Optional<String> exceptionResponseMessage = responseBuilder.getJsonResponseString(response);

        switch (response.getStatus()) {
            case SC_UNAUTHORIZED:
            case SC_FORBIDDEN:
                final String forbiddenOrUnauthorized = exceptionResponseMessage.orElse("FORBIDDEN or UNAUTHORIZED");
                LOGGER.warn("Exception occurred while calling camunda engine. status code {}, responsePayload {}", response.getStatus(), forbiddenOrUnauthorized);
                throw new ForbiddenRequestException(forbiddenOrUnauthorized);

            case SC_NOT_FOUND:
            case SC_BAD_REQUEST:
                final String notFoundOrBadRequest = exceptionResponseMessage.orElse("NOT_FOUND or BAD_REQUEST");
                LOGGER.warn("Exception occurred while calling camunda engine. status code {}, responsePayload {}", response.getStatus(), notFoundOrBadRequest);
                throw new BadRequestException(exceptionResponseMessage.orElse("NOT_FOUND or BAD_REQUEST"));

            default:
                final String serverError = exceptionResponseMessage.orElse("Exception Occurred while calling camunda!");
                LOGGER.warn("Exception occurred while calling camunda engine. status code {}, responsePayload {}", response.getStatus(), serverError);
                throw new RuntimeException(serverError);
        }

    }
}
