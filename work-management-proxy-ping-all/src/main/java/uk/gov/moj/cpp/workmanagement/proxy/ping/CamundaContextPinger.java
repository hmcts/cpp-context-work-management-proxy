package uk.gov.moj.cpp.workmanagement.proxy.ping;

import static java.lang.String.format;

import uk.gov.moj.cpp.workmanagement.proxy.ping.camunda.CamundaHealthcheck;
import uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.ContextPingLocation;
import uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.ContextPingLocationProvider;
import uk.gov.moj.cpp.workmanagement.proxy.ping.http.ContextHttpPingException;
import uk.gov.moj.cpp.workmanagement.proxy.ping.http.HttpContextPingerClient;

import javax.inject.Inject;

import org.slf4j.Logger;

public class CamundaContextPinger {

    @Inject
    private CamundaHealthcheck camundaHealthcheck;

    @Inject
    private ContextPingLocationProvider contextPingLocationProvider;

    @Inject
    private HttpContextPingerClient httpContextPingerClient;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    public String pingCamundaAndAllOtherCamundaContexts() {

        camundaHealthcheck.checkForActiveCamundaProcessInstance();

        contextPingLocationProvider.getContextPingLocations()
                .forEach(this::pingContext);

        return "pong";
    }

    @SuppressWarnings("squid:S2629")
    private void pingContext(final ContextPingLocation contextPingLocation) {

        final String contextName = contextPingLocation.getContextName();
        logger.debug(format("Checking for availability of '%s' context", contextName));
        final String response = httpContextPingerClient.ping(contextPingLocation);

        if(! "pong".equals(response)) {
            throw new ContextHttpPingException(format(
                    "Ping failed for context '%s': ping returned '%s'",
                    contextName,
                    response));
        }

        logger.debug(format("Ping to '%s' successfully returned with a '%s'", contextName, response));
    }
}
