package uk.gov.moj.cpp.workmanagement.proxy.ping.configuration;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class ContextPingLocationProviderTest {

    @Mock
    private CamundaContextPingerConfiguration camundaContextPingerConfiguration;

    @Mock
    private Logger logger;

    @InjectMocks
    private ContextPingLocationProvider contextPingLocationProvider;

    @Test
    public void shouldParseTheJndiConfigurationVariableIntoListOfContextPingLocations() throws Exception {

        final String contextName_1 = "contextName_1";
        final String contextName_2 = "contextName_2";
        final int port_1 = 8081;
        final String hostName_1 = "hostName_1";
        final String hostName_2 = "hostName_2";
        final int port_2 = 8082;

        final String locationsString = format("%s:%s:%d,%s:%s:%d",
                contextName_1,
                hostName_1,
                port_1,
                contextName_2,
                hostName_2,
                port_2);

        when(camundaContextPingerConfiguration.getContextPingLocations()).thenReturn(locationsString);

        final List<ContextPingLocation> contextPingLocations = contextPingLocationProvider.getContextPingLocations();

        assertThat(contextPingLocations.size(), is(2));

        assertThat(contextPingLocations.get(0), is(new ContextPingLocation(contextName_1, hostName_1, port_1)));
        assertThat(contextPingLocations.get(1), is(new ContextPingLocation(contextName_2, hostName_2, port_2)));
    }

    @Test
    public void shouldHandleSingleLocation() throws Exception {
        final String contextName = "contextName";
        final String hostName = "hostName";
        final int port = 8081;

        final String locationsString = format("%s:%s:%d",
                contextName,
                hostName,
                port);

        when(camundaContextPingerConfiguration.getContextPingLocations()).thenReturn(locationsString);

        final List<ContextPingLocation> contextPingLocations = contextPingLocationProvider.getContextPingLocations();

        assertThat(contextPingLocations.size(), is(1));

        assertThat(contextPingLocations.get(0), is(new ContextPingLocation(contextName, hostName, port)));
    }


    @Test
    public void shouldHandleUseDefaultPortNumberIfNoneSet() throws Exception {
        final String contextName = "contextName";
        final String hostName = "hostName";

        final String locationsString = format("%s:%s",
                contextName,
                hostName);

        when(camundaContextPingerConfiguration.getContextPingLocations()).thenReturn(locationsString);

        final List<ContextPingLocation> contextPingLocations = contextPingLocationProvider.getContextPingLocations();

        assertThat(contextPingLocations.size(), is(1));
        assertThat(contextPingLocations.get(0), is(new ContextPingLocation(contextName, hostName, 8080)));
    }

    @Test
    public void shouldUseDefaultPortIfPortCannotBeParsed() throws Exception {
        final String contextName = "contextName";
        final String hostName = "hostName";
        final String notAnIntegerPort = "not-an-integer";

        final String locationsString = format("%s:%s:%s",
                contextName,
                hostName,
                notAnIntegerPort);

        when(camundaContextPingerConfiguration.getContextPingLocations()).thenReturn(locationsString);

        final List<ContextPingLocation> contextPingLocations = contextPingLocationProvider.getContextPingLocations();

        assertThat(contextPingLocations.size(), is(1));
        assertThat(contextPingLocations.get(0), is(new ContextPingLocation(contextName, hostName, 8080)));

        verify(logger).warn("Malformed ping location. Port number 'not-an-integer' is not an integer. Using default of '8080'");
    }

    @Test
    public void shouldThrowMalformedContextPingLocationsJndiValueExceptionIfLocationsStringCannotBeParsed() throws Exception {
        final String badLocationsString = "oh-dear-me";

        when(camundaContextPingerConfiguration.getContextPingLocations()).thenReturn(badLocationsString);

        final MalformedContextPingLocationsJndiValueException malformedContextPingLocationsJndiValueException
                = assertThrows(MalformedContextPingLocationsJndiValueException.class,
                () -> contextPingLocationProvider.getContextPingLocations());

        assertThat(malformedContextPingLocationsJndiValueException.getMessage(), is("The JNDI variable 'contextPingLocations' should be in the form '<context-name>:<host-name>,<context-name>:<host-name>' but was 'oh-dear-me'"));
        assertThat(malformedContextPingLocationsJndiValueException.getCause(), is(instanceOf(ArrayIndexOutOfBoundsException.class)));
    }
}