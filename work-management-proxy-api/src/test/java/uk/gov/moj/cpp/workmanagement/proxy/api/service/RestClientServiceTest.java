package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class RestClientServiceTest {

    public static final String HTTP_LOCALHOST = "http://localhost/";
    public static final String REQUEST_BODY = "{}";

    @Mock
    private Client client;
    @Mock
    private WebTarget target;
    @Mock
    private Invocation.Builder builder;
    @Mock
    private Response response;

    @InjectMocks
    private RestClientService restClientService;

    @Test
    public void shouldPerformGetRequestWithQueryString() {
        final String queryString = "name=assigneename";
        final String urlQueryString = HTTP_LOCALHOST + "?" + queryString;
        when(client.target(urlQueryString)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.accept(MediaType.APPLICATION_JSON)).thenReturn(builder);
        when(builder.get()).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(OK);
        
        final Response newResponse = restClientService.get(HTTP_LOCALHOST, queryString);
        verify(client).target(urlQueryString);
        assertThat(newResponse.getStatusInfo(), is(OK));
    }


    @Test
    public void shouldPerformPostRequestWithRequestBodyAsString() {
        Entity<String> entity = Entity.entity(REQUEST_BODY, MediaType.APPLICATION_JSON);
        when(client.target(HTTP_LOCALHOST)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.accept(MediaType.APPLICATION_JSON)).thenReturn(builder);
        when(builder.post(any(entity.getClass()))).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(OK);

        final Response newResponse = restClientService.post(HTTP_LOCALHOST, REQUEST_BODY);
        verify(client).target(HTTP_LOCALHOST);
        assertThat(newResponse.getStatusInfo(), is(OK));
    }

    @Test
    public void shouldPerformPutRequestWithRequestBodyAsString() {
        Entity<String> entity = Entity.entity(REQUEST_BODY, MediaType.APPLICATION_JSON);
        when(client.target(HTTP_LOCALHOST)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.accept(MediaType.APPLICATION_JSON)).thenReturn(builder);
        when(builder.put(any(entity.getClass()))).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(OK);

        final Response newResponse = restClientService.put(HTTP_LOCALHOST, REQUEST_BODY);
        verify(client).target(HTTP_LOCALHOST);
        assertThat(newResponse.getStatusInfo(), is(OK));
    }

    @Test
    public void shouldPerformDeleteRequestWithDeleteIdAsQueryString() {
        final String urlQueryString = HTTP_LOCALHOST + "?id=deleteid";
        when(client.target(urlQueryString)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.accept(MediaType.APPLICATION_JSON)).thenReturn(builder);
        when(builder.delete()).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(OK);

        final Response newResponse = restClientService.delete(urlQueryString);
        verify(client).target(urlQueryString);
        assertThat(newResponse.getStatusInfo(), is(OK));
    }

}