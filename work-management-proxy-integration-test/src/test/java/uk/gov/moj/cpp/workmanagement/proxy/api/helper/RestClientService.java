package uk.gov.moj.cpp.workmanagement.proxy.api.helper;

import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestClientService extends RestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientService.class);

    public RestClientService(){
        super();
    }

    public Response putCommand(final String url, final String contentType, final String requestPayload) {
        final Entity<String> entity = Entity.entity(requestPayload, MediaType.valueOf(contentType));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Making PUT request to '{}' with Content Type '{}'", url, contentType);
            LOGGER.info("Request payload: '{}'", requestPayload);
        }

        final Response response = ResteasyClientBuilderFactory.clientBuilder().build().target(url).request().put(entity);
        if (LOGGER.isInfoEnabled()) {
            Response.StatusType statusType = response.getStatusInfo();
            LOGGER.info("Received response status '{}' '{}'", statusType.getStatusCode(), statusType.getReasonPhrase());
        }

        return response;
    }

    public Response putCommand(final String url, final String contentType, final String requestPayload, final MultivaluedMap<String, Object> headers) {
        final Entity<String> entity = Entity.entity(requestPayload, MediaType.valueOf(contentType));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Making PUT request to '{}' with Content Type '{}'", url, contentType);
            LOGGER.info("Request payload: '{}'", requestPayload);
        }

        final Response response = ResteasyClientBuilderFactory.clientBuilder().build().target(url).request().headers(headers).put(entity);
        if (LOGGER.isInfoEnabled()) {
            Response.StatusType statusType = response.getStatusInfo();
            LOGGER.info("Received response status '{}' '{}'", statusType.getStatusCode(), statusType.getReasonPhrase());
        }

        return response;
    }
}
