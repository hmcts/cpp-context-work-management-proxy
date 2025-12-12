package uk.gov.moj.cpp.workmanagement.proxy.ping.configuration;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.CamundaContextPingerConfiguration.CONTEXT_PING_LOCATIONS_JNDI_NAME;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;

public class ContextPingLocationProvider {

    private static final int DEFAULT_PORT = 8080;

    @Inject
    private CamundaContextPingerConfiguration camundaContextPingerConfiguration;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    public List<ContextPingLocation> getContextPingLocations() {

        final String contextPingLocationsJndiValue = camundaContextPingerConfiguration.getContextPingLocations();

        try {
            return stream(contextPingLocationsJndiValue.split(","))
                    .map(this::asContextPingLocation)
                    .collect(toList());
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new MalformedContextPingLocationsJndiValueException(
                    format("The JNDI variable '%s' should be in the form '<context-name>:<host-name>,<context-name>:<host-name>' but was '%s'",
                            CONTEXT_PING_LOCATIONS_JNDI_NAME,
                            contextPingLocationsJndiValue),
                    e);
        }
    }

    private ContextPingLocation asContextPingLocation(final String location) {

        final String[] contextAndHostName = location.split(":");


        return new ContextPingLocation(
                contextAndHostName[0],
                contextAndHostName[1],
                getPort(contextAndHostName));
    }

    private int getPort(final String[] contextAndHostName) {
        if (contextAndHostName.length > 2) {
            final String portString = contextAndHostName[2];
            try {
                return parseInt(portString);
            } catch (final NumberFormatException e) {
                logger.warn(format("Malformed ping location. Port number '%s' is not an integer. Using default of '%d'", portString, DEFAULT_PORT));
            }
        }

        return DEFAULT_PORT;
    }
}
