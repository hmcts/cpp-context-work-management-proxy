package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2221")
public class UserService {

    @Inject
    @ServiceComponent("WorkManagement.Proxy.API")
    private Requester requester;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private static final String USER_ID = "userId";

    public String getUserDetails(final String userId) {
        try {
            final Envelope<JsonObject> response = getUserDetailsAsAdmin(userId);
            final JsonObject jsonObject = response.payload();
            return jsonObject != null ? jsonObject.getString("firstName") + " " + jsonObject.getString("lastName") : userId;
        } catch (Exception ex) {
            LOGGER.error("User details could not find ", ex);
            return userId;
        }
    }

    private Envelope<JsonObject> getUserDetailsAsAdmin(final String userId) {
        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withUserId(userId)
                .withName("usersgroups.get-user-details");

        return requester.requestAsAdmin(envelopeFrom(metadataBuilder, createObjectBuilder().add(USER_ID, userId).build()), JsonObject.class);
    }
}
