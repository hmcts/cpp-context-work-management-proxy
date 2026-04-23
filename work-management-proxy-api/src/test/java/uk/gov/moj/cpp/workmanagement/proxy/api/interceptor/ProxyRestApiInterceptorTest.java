package uk.gov.moj.cpp.workmanagement.proxy.api.interceptor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.workmanagement.proxy.api.exception.ExceptionProvider;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.RestClientService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.WorkManagementResponseBuilder;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProxyRestApiInterceptorTest {

    public static final String CAMUNDA_BASE_URL = "http://localhost:8080/engine-rest";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String TASK_SUB_PATH = "/task";
    public static final String COMPLETE_WORKMANAGEMENTPROXY_TASK_URL = "http://localhost:8080/work-management-proxy-api/rest/workmanagementproxy/task";
    public static final String METADATA_ID = "692fa43d-4a80-4925-ac2c-1a79e6ec8660";
    public static final String METADATA_NAME = "direct-camunda-call";
    public static final String SOME_PAYLOAD = "somePayload";
    public static final String PAYLOAD_FIELD = "payload";
    private static final String SERVLET_PATH = "/rest/workmanagementproxy";
    @Mock
    protected RestClientService restClientService;
    @Mock
    protected InterceptorContext interceptorContext;
    @Mock
    protected InterceptorChain interceptorChain;
    @Mock
    protected JsonEnvelope jsonEnvelope;
    @Mock
    protected Metadata metadata;
    @InjectMocks
    protected ProxyRestApiInterceptor proxyRestApiInterceptor;
    @Captor
    protected ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;
    @Mock
    private WorkManagementResponseBuilder responseBuilder;
    @Mock
    private ExceptionProvider exceptionProvider;
    @Mock
    private HttpServletRequest request;

    @Mock
    private Response response;

    @BeforeEach
    public void setUp() {
        ReflectionUtil.setField(proxyRestApiInterceptor, "camundaBaseUrl", CAMUNDA_BASE_URL);

        Metadata metadata = DefaultJsonMetadata.metadataBuilderFrom(createJsonMetaData()).build();
        when(interceptorContext.inputEnvelope()).thenReturn(jsonEnvelope);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(request.getRequestURL()).thenReturn(new StringBuffer().append(COMPLETE_WORKMANAGEMENTPROXY_TASK_URL));
    }

    @Test
    public void shouldPerformCamundaGetForMethodGet() {

        when(request.getMethod()).thenReturn(METHOD_GET);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);

        when(restClientService.get(any(), any())).thenReturn(response);

        final JsonValue jsonValue = createObjectBuilder().add("payload", SOME_PAYLOAD).build();
        when(responseBuilder.getResponsePayload(response)).thenReturn(jsonValue);
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        proxyRestApiInterceptor.process(interceptorContext, interceptorChain);

        verify(restClientService).get(CAMUNDA_BASE_URL + TASK_SUB_PATH, null);
        verify(interceptorContext).copyWithOutput(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertEquals(METADATA_NAME, jsonEnvelope.metadata().name());
        assertEquals(METADATA_ID, jsonEnvelope.metadata().id().toString());
        assertEquals(SOME_PAYLOAD, jsonEnvelope.payloadAsJsonObject().getString(PAYLOAD_FIELD));
    }


    @Test
    public void shouldPerformCamundaPostForMethodPost() {
        final JsonObject payload = createJsonPayload();
        when(request.getMethod()).thenReturn(METHOD_POST);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);

        when(restClientService.post(any(String.class), any(String.class))).thenReturn(response);

        final JsonValue jsonValue = createObjectBuilder().add("payload", SOME_PAYLOAD).build();
        when(responseBuilder.getResponsePayload(response)).thenReturn(jsonValue);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createJsonPayload());
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        proxyRestApiInterceptor.process(interceptorContext, interceptorChain);

        verify(restClientService).post(CAMUNDA_BASE_URL + TASK_SUB_PATH, payload.toString());
        verify(interceptorContext).copyWithOutput(jsonEnvelopeArgumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertEquals(METADATA_NAME, jsonEnvelope.metadata().name());
        assertEquals(METADATA_ID, jsonEnvelope.metadata().id().toString());

        assertEquals(SOME_PAYLOAD, jsonEnvelope.payloadAsJsonObject().getString(PAYLOAD_FIELD));

    }


    @Test
    public void shouldPerformCamundaPutForMethodPut() {
        final JsonObject payload = createJsonPayload();

        when(request.getMethod()).thenReturn(METHOD_PUT);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);

        when(restClientService.put(any(String.class), any(String.class))).thenReturn(response);

        final JsonValue jsonValue = createObjectBuilder().add("payload", SOME_PAYLOAD).build();
        when(responseBuilder.getResponsePayload(response)).thenReturn(jsonValue);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createJsonPayload());
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        proxyRestApiInterceptor.process(interceptorContext, interceptorChain);
        verify(restClientService).put(CAMUNDA_BASE_URL + TASK_SUB_PATH, payload.toString());

        verify(interceptorContext).copyWithOutput(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertEquals(METADATA_NAME, jsonEnvelope.metadata().name());
        assertEquals(METADATA_ID, jsonEnvelope.metadata().id().toString());
        assertEquals(SOME_PAYLOAD, jsonEnvelope.payloadAsJsonObject().getString(PAYLOAD_FIELD));
    }

    @Test
    public void shouldPerformCamundaDeleteForMethodDelete() {

        when(request.getMethod()).thenReturn(METHOD_DELETE);

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);

        when(restClientService.delete(any(String.class))).thenReturn(response);
        final JsonValue jsonValue = createObjectBuilder().add("payload", SOME_PAYLOAD).build();
        when(responseBuilder.getResponsePayload(response)).thenReturn(jsonValue);
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        proxyRestApiInterceptor.process(interceptorContext, interceptorChain);
        verify(restClientService).delete(CAMUNDA_BASE_URL + TASK_SUB_PATH);

        verify(interceptorContext).copyWithOutput(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertEquals(METADATA_NAME, jsonEnvelope.metadata().name());
        assertEquals(METADATA_ID, jsonEnvelope.metadata().id().toString());
        assertEquals(SOME_PAYLOAD, jsonEnvelope.payloadAsJsonObject().getString(PAYLOAD_FIELD));
    }


    @Test
    public void shouldThrowNotAllowedWhenMethodNotAllowed() {
        when(request.getMethod()).thenReturn("PATCH");
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        assertThrows(BadRequestException.class,
                () -> proxyRestApiInterceptor.process(interceptorContext, interceptorChain));
    }

    @Test
    public void shouldThrowBadRequestExceptionFor400Error() {

        when(request.getMethod()).thenReturn(METHOD_PUT);
        when(response.getStatus()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        doThrow(BadRequestException.class).when(exceptionProvider).throwApplicationSpecificException(any());
        when(restClientService.put(any(String.class), any(String.class))).thenReturn(response);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createJsonPayload());
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        assertThrows(BadRequestException.class,
                () -> proxyRestApiInterceptor.process(interceptorContext, interceptorChain));
    }

    @Test
    public void shouldThrowBadRequestExceptionForOther400Error() {

        when(request.getMethod()).thenReturn(METHOD_PUT);
        when(response.getStatus()).thenReturn(HttpStatus.SC_REQUEST_TIMEOUT);
        doThrow(RuntimeException.class).when(exceptionProvider).throwApplicationSpecificException(any());

        when(restClientService.put(any(String.class), any(String.class))).thenReturn(response);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createJsonPayload());
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        assertThrows(RuntimeException.class,
                () -> proxyRestApiInterceptor.process(interceptorContext, interceptorChain));
    }

    @Test
    public void shouldThrowRuntimeExceptionFor500Error() {
        when(request.getMethod()).thenReturn(METHOD_PUT);
        when(response.getStatus()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        doThrow(RuntimeException.class).when(exceptionProvider).throwApplicationSpecificException(any());

        when(restClientService.put(any(String.class), any(String.class))).thenReturn(response);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createJsonPayload());
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        assertThrows(RuntimeException.class,
                () -> proxyRestApiInterceptor.process(interceptorContext, interceptorChain));
    }


    @Test
    public void shouldThrowNotFoundExceptionForNotFound() {
        when(request.getMethod()).thenReturn(METHOD_PUT);
        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        doThrow(BadRequestException.class).when(exceptionProvider).throwApplicationSpecificException(any());

        when(restClientService.put(any(String.class), any(String.class))).thenReturn(response);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createJsonPayload());
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        assertThrows(BadRequestException.class,
                () -> proxyRestApiInterceptor.process(interceptorContext, interceptorChain));
    }


    @Test
    public void shouldThrowNotAuthorizedExceptionForUnAuthorized() {
        when(request.getMethod()).thenReturn(METHOD_PUT);
        when(response.getStatus()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        doThrow(ForbiddenRequestException.class).when(exceptionProvider).throwApplicationSpecificException(any());

        when(restClientService.put(any(String.class), any(String.class))).thenReturn(response);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createJsonPayload());
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        assertThrows(ForbiddenRequestException.class,
                () -> proxyRestApiInterceptor.process(interceptorContext, interceptorChain));
    }

    @Test
    public void shouldThrowForbiddenExceptionForForbidden() {

        when(request.getMethod()).thenReturn(METHOD_PUT);
        when(response.getStatus()).thenReturn(HttpStatus.SC_FORBIDDEN);
        doThrow(ForbiddenRequestException.class).when(exceptionProvider).throwApplicationSpecificException(any());
        when(restClientService.put(any(String.class), any(String.class))).thenReturn(response);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createJsonPayload());
        when(request.getServletPath()).thenReturn(SERVLET_PATH);

        assertThrows(ForbiddenRequestException.class,
                () -> proxyRestApiInterceptor.process(interceptorContext, interceptorChain));
    }

    private JsonObject createJsonPayload() {
        return createObjectBuilder()
                .add(PAYLOAD_FIELD, SOME_PAYLOAD)
                .build();
    }

    private JsonObject createJsonMetaData() {
        return createObjectBuilder()
                .add("name", METADATA_NAME)
                .add("id", METADATA_ID)
                .build();

    }

}