package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.JsonUtils.isJsonValid;

import java.io.StringReader;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;

@ApplicationScoped
public class WorkManagementResponseBuilder {

    private static final String RESULTS = "results";

    /**
     * Fudging array response to be returned as json object response
     *
     * @param response
     * @return
     */
    public JsonValue getResponsePayload(final Response response) {

        if (response.getStatus() != HttpStatus.SC_OK) {
            return createObjectBuilder().build();
        }

        final Optional<String> jsonResponseString = getJsonResponseString(response);
        if (!jsonResponseString.isPresent()) {
            return createObjectBuilder().build();
        }

        try (final JsonReader reader = createReader(new StringReader(jsonResponseString.get()))) {
            final JsonStructure responseJsonStructure = reader.read();
            if (responseJsonStructure instanceof JsonArray) {
                return createObjectBuilder().add(RESULTS, responseJsonStructure).build();
            }
            return responseJsonStructure;
        }

    }

    public Optional<String> getJsonResponseString(final Response response) {
        final String responseAsString = response.readEntity(String.class);
        if (nonNull(responseAsString) && isJsonValid(responseAsString)) {
            return Optional.ofNullable(responseAsString);
        }
        return empty();
    }
}
