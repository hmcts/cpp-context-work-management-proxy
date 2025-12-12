package uk.gov.moj.cpp.workmanagement.proxy.ping.configuration;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;


public class CamundaContextPingerConfiguration {

    public static final String CONTEXT_PING_LOCATIONS_JNDI_NAME = "contextPingLocations";

    @Inject
    @Value(key = CONTEXT_PING_LOCATIONS_JNDI_NAME, defaultValue = "work-management-proxy-api:localhost")
    private String contextPingLocations;

    public String getContextPingLocations() {
        return contextPingLocations;
    }
}
