package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkManagementResponseBuilderTest {


    private static final String VALID_JSON_SAMPLE = "{\"key\":\"value\"}";
    private static final String INVALID_JSON_SAMPLE = "{\"key\":value\"}";
    private static final String EMPTY_JSON = "{}";

    @InjectMocks
    protected WorkManagementResponseBuilder workManagementResponseBuilder;

    @Mock
    private Response response;

    @Test
    public void shouldReturnValidJsonStringAsResponsePayloadContainsValidJson() {

        when(response.readEntity(String.class)).thenReturn(VALID_JSON_SAMPLE);
        final Optional<String> jsonResponseString = workManagementResponseBuilder.getJsonResponseString(response);
        assertEquals(VALID_JSON_SAMPLE, jsonResponseString.get());

    }

    @Test
    public void shouldReturnEmptyJsonStringWhenResponsePayloadContainsInvalidJson() {

        when(response.readEntity(String.class)).thenReturn(INVALID_JSON_SAMPLE);
        final Optional<String> jsonResponseString = workManagementResponseBuilder.getJsonResponseString(response);
        assertFalse(jsonResponseString.isPresent());

    }

    @Test
    public void shouldReturnEmptyJsonStringWhenResponsePayloadIsNull() {

        when(response.readEntity(String.class)).thenReturn(null);
        Optional<String> jsonResponseString = workManagementResponseBuilder.getJsonResponseString(response);
        assertFalse(jsonResponseString.isPresent());

    }


    @Test
    public void shouldReturnValidJsonValueWhenResponseStatusIsOKAndContainsValidJson() {

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.readEntity(String.class)).thenReturn(VALID_JSON_SAMPLE);
        JsonValue jsonValue = workManagementResponseBuilder.getResponsePayload(response);
        assertEquals(VALID_JSON_SAMPLE, jsonValue.toString());

    }

    @Test
    public void shouldReturnEmptyJsonValueWhenResponseStatusIsOKAndContainsInvalidJson() {

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.readEntity(String.class)).thenReturn(INVALID_JSON_SAMPLE);
        JsonValue jsonValue = workManagementResponseBuilder.getResponsePayload(response);
        assertEquals(EMPTY_JSON, jsonValue.toString());

    }

    @Test
    public void shouldReturnEmptyJsonValueWhenResponseStatusIsAcceptedAndContainsValidJson() {

        when(response.getStatus()).thenReturn(HttpStatus.SC_ACCEPTED);
        JsonValue jsonValue = workManagementResponseBuilder.getResponsePayload(response);
        assertEquals(EMPTY_JSON, jsonValue.toString());

    }

    @Test
    public void shouldReturnValidJsonArrayValueWhenResponseStatusIsOKAndContainsArrayJson() {

        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.readEntity(String.class)).thenReturn("[\"a\", \"b\"]");
        JsonValue jsonValue = workManagementResponseBuilder.getResponsePayload(response);
        assertEquals("{\"results\":[\"a\",\"b\"]}", jsonValue.toString());

    }

}