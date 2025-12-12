package uk.gov.moj.cpp.workmanagement.proxy.ping.http;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.workmanagement.proxy.ping.PingLocationToUrlGenerator;
import uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.ContextPingLocation;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HttpContextPingerClientTest {

    @Mock
    private PingLocationToUrlGenerator pingLocationToUrlGenerator;

    @Mock
    private PingerClientDelegate pingerClientDelegate;

    @InjectMocks
    private HttpContextPingerClient httpContextPingerClient;

    @Test
    public void shouldPingAnotherContext() throws Exception {

        final String url = "http://some-hostname:1234/some-context/internal/metrics/ping";
        final String responseString = "pong";
        final ContextPingLocation contextPingLocation = new ContextPingLocation(
                "some-context",
                "some-hostname",
                1234
        );

        final CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        final HttpGet httpGet = mock(HttpGet.class);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final HttpEntity httpEntity = mock(HttpEntity.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(pingLocationToUrlGenerator.asUrlString(contextPingLocation)).thenReturn(url);
        when(pingerClientDelegate.createDefaultClient()).thenReturn(closeableHttpClient);
        when(pingerClientDelegate.asHttpGet(url)).thenReturn(httpGet);
        when(closeableHttpClient.execute(httpGet)).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(response.getEntity()).thenReturn(httpEntity);
        when(pingerClientDelegate.asString(httpEntity)).thenReturn(responseString + "\n");

        assertThat(httpContextPingerClient.ping(contextPingLocation), is(responseString));

        verify(closeableHttpClient).close();
        verify(response).close();
    }

    @Test
    public void shouldFailIfPingingAnotherContextFails() throws Exception {

        final String url = "http://some-hostname:1234/some-context/internal/metrics/ping";
        final ContextPingLocation contextPingLocation = new ContextPingLocation(
                "some-context",
                "some-hostname",
                1234
        );

        final CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        final HttpGet httpGet = mock(HttpGet.class);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final HttpEntity httpEntity = mock(HttpEntity.class);
        final StatusLine statusLine = mock(StatusLine.class);
        final IOException ioException = new IOException("Ooops");

        when(pingLocationToUrlGenerator.asUrlString(contextPingLocation)).thenReturn(url);
        when(pingerClientDelegate.createDefaultClient()).thenReturn(closeableHttpClient);
        when(pingerClientDelegate.asHttpGet(url)).thenReturn(httpGet);
        when(closeableHttpClient.execute(httpGet)).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(response.getEntity()).thenReturn(httpEntity);
        when(pingerClientDelegate.asString(httpEntity)).thenThrow(ioException);

        final ContextHttpPingException contextHttpPingException = assertThrows(
                ContextHttpPingException.class,
                () -> httpContextPingerClient.ping(contextPingLocation));

        assertThat(contextHttpPingException.getCause(),is(ioException));
        assertThat(contextHttpPingException.getMessage(), is("Failed to make http 'ping' request to 'http://some-hostname:1234/some-context/internal/metrics/ping'"));

        verify(closeableHttpClient).close();
        verify(response).close();
    }

    @Test
    public void shouldFailIfPingingAnotherContextReturnsNull() throws Exception {

        final String url = "http://some-hostname:1234/some-context/internal/metrics/ping";
        final ContextPingLocation contextPingLocation = new ContextPingLocation(
                "some-context",
                "some-hostname",
                1234
        );

        final CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        final HttpGet httpGet = mock(HttpGet.class);
        final StatusLine statusLine = mock(StatusLine.class);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);

        when(pingLocationToUrlGenerator.asUrlString(contextPingLocation)).thenReturn(url);
        when(pingerClientDelegate.createDefaultClient()).thenReturn(closeableHttpClient);
        when(pingerClientDelegate.asHttpGet(url)).thenReturn(httpGet);
        when(closeableHttpClient.execute(httpGet)).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(response.getEntity()).thenReturn(null);

        final ContextHttpPingException contextHttpPingException = assertThrows(
                ContextHttpPingException.class,
                () -> httpContextPingerClient.ping(contextPingLocation));

        assertThat(contextHttpPingException.getMessage(), is("Http 'ping' request to 'http://some-hostname:1234/some-context/internal/metrics/ping' returned null as response"));

        verify(closeableHttpClient).close();
        verify(response).close();
    }

    @Test
    public void shouldFailIfPingingAnotherContextDoesNotReturn200() throws Exception {

        final String url = "http://some-hostname:1234/some-context/internal/metrics/ping";
        final ContextPingLocation contextPingLocation = new ContextPingLocation(
                "some-context",
                "some-hostname",
                1234
        );
        final int badStatusCode = 500;
        final String badReasonPhrase = "Internal Server Error";

        final CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        final HttpGet httpGet = mock(HttpGet.class);
        final StatusLine statusLine = mock(StatusLine.class);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);

        when(pingLocationToUrlGenerator.asUrlString(contextPingLocation)).thenReturn(url);
        when(pingerClientDelegate.createDefaultClient()).thenReturn(closeableHttpClient);
        when(pingerClientDelegate.asHttpGet(url)).thenReturn(httpGet);
        when(closeableHttpClient.execute(httpGet)).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(badStatusCode);
        when(statusLine.getReasonPhrase()).thenReturn(badReasonPhrase);

        final ContextHttpPingException contextHttpPingException = assertThrows(
                ContextHttpPingException.class,
                () -> httpContextPingerClient.ping(contextPingLocation));

        assertThat(contextHttpPingException.getMessage(), is("Ping failed for 'http://some-hostname:1234/some-context/internal/metrics/ping': 500 'Internal Server Error'"));

        verify(closeableHttpClient).close();
        verify(response).close();
    }
}