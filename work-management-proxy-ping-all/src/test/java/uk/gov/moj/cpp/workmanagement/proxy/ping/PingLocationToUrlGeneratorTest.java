package uk.gov.moj.cpp.workmanagement.proxy.ping;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.ContextPingLocation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class PingLocationToUrlGeneratorTest {


    @InjectMocks
    private PingLocationToUrlGenerator pingLocationToUrlGenerator;

    @Test
    public void shouldGenerateCorrectPingLocationUrl() throws Exception {

        final ContextPingLocation contextPingLocation = new ContextPingLocation(
                "some-context",
                "some-hostname",
                1234
        );

        assertThat(pingLocationToUrlGenerator.asUrlString(contextPingLocation), is("http://some-hostname:1234/some-context/internal/metrics/ping"));
    }
}