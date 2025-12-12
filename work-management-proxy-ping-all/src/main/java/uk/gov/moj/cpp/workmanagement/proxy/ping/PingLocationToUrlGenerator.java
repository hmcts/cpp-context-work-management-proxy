package uk.gov.moj.cpp.workmanagement.proxy.ping;

import uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.ContextPingLocation;


public class PingLocationToUrlGenerator {

    public String asUrlString(final ContextPingLocation contextPingLocation) {

        return String.format(
                "http://%s:%d/%s/internal/metrics/ping",
                contextPingLocation.getHostName(),
                contextPingLocation.getPort(),
                contextPingLocation.getContextName());
    }
}
