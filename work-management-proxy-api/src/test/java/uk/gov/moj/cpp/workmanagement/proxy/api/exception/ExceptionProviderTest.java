package uk.gov.moj.cpp.workmanagement.proxy.api.exception;

import static java.util.Optional.of;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.WorkManagementResponseBuilder;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExceptionProviderTest {

    @InjectMocks
    protected ExceptionProvider exceptionProvider;
    @Mock
    private WorkManagementResponseBuilder responseBuilder;
    @Mock
    private Response response;

    @BeforeEach
    public void setUp() {
        when(responseBuilder.getJsonResponseString(response)).thenReturn(of("SOME STANDARD EXCEPTION MESSAGE"));
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenResponseStatusIs401() {
        when(response.getStatus()).thenReturn(SC_UNAUTHORIZED);
        assertThrows(ForbiddenRequestException.class, () -> exceptionProvider.throwApplicationSpecificException(response));
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenResponseStatusIs403() {
        when(response.getStatus()).thenReturn(SC_FORBIDDEN);
        assertThrows(ForbiddenRequestException.class, () -> exceptionProvider.throwApplicationSpecificException(response));
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenResponseStatusIs400() {
        when(response.getStatus()).thenReturn(SC_BAD_REQUEST);
        assertThrows(BadRequestException.class, () -> exceptionProvider.throwApplicationSpecificException(response));
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenResponseStatusIs404() {
        when(response.getStatus()).thenReturn(SC_NOT_FOUND);
        assertThrows(BadRequestException.class, () -> exceptionProvider.throwApplicationSpecificException(response));
    }

    @Test
    public void shouldThrowRuntimeExceptionWhenResponseStatusIsGreaterThan404() {
        when(response.getStatus()).thenReturn(HttpStatus.SC_METHOD_NOT_ALLOWED);
        assertThrows(RuntimeException.class, () -> exceptionProvider.throwApplicationSpecificException(response));
    }

    @Test
    public void shouldThrowRuntimeExceptionWhenResponseStatusIs500() {
        when(response.getStatus()).thenReturn(SC_INTERNAL_SERVER_ERROR);
        assertThrows(RuntimeException.class, () -> exceptionProvider.throwApplicationSpecificException(response));
    }

}