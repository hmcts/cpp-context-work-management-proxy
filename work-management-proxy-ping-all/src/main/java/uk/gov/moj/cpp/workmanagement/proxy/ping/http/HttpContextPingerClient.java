package uk.gov.moj.cpp.workmanagement.proxy.ping.http;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;

import uk.gov.moj.cpp.workmanagement.proxy.ping.PingLocationToUrlGenerator;
import uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.ContextPingLocation;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class HttpContextPingerClient {

    private PingLocationToUrlGenerator pingLocationToUrlGenerator;
    private final PingerClientDelegate pingerClientDelegate;

    @Inject
    public HttpContextPingerClient(final PingLocationToUrlGenerator pingLocationToUrlGenerator, final PingerClientDelegate pingerClientDelegate) {
        this.pingLocationToUrlGenerator = pingLocationToUrlGenerator;
        this.pingerClientDelegate = pingerClientDelegate;
    }

    public String ping(final ContextPingLocation contextPingLocation) {

        final String url = pingLocationToUrlGenerator.asUrlString(contextPingLocation);

        try(final CloseableHttpClient httpClient = pingerClientDelegate.createDefaultClient()) {

            final HttpGet httpGet = pingerClientDelegate.asHttpGet(url);

            try (final CloseableHttpResponse response = httpClient.execute(httpGet)){

                final StatusLine statusLine = response.getStatusLine();

                final int statusCode = statusLine.getStatusCode();
                if (statusCode != SC_OK) {
                    throw new ContextHttpPingException(format(
                            "Ping failed for '%s': %d '%s'",
                            url,
                            statusCode,
                            statusLine.getReasonPhrase()
                    ));
                }

                final HttpEntity httpEntity = response.getEntity();
                if (httpEntity == null) {
                    throw new ContextHttpPingException(format("Http 'ping' request to '%s' returned null as response", url));
                }

                return pingerClientDelegate.asString(httpEntity).trim();
            }
        } catch (final IOException e) {
            throw new ContextHttpPingException(format("Failed to make http 'ping' request to '%s'", url), e);
        }
    }
}
