package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class UsersServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private Requester requester;

    private static final String USER_ID = randomUUID().toString();

    @Test
    void shouldGetUserDetails() {
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithRandomUUID("usersgroups.get-user-details")
                .withUserId(USER_ID), createObjectBuilder().build());

       final JsonObject jsonObject = createObjectBuilder()
                .add("firstName", "Robin")
                .add("lastName", "Hood")
                .build();

        when(requester.requestAsAdmin(any(), any())).thenReturn(Envelope.envelopeFrom(requestEnvelope.metadata(), jsonObject));

        final String response = userService.getUserDetails(USER_ID);

        verify(requester).requestAsAdmin(any(), any());
        assertThat(response, is("Robin Hood"));
    }

    @Test
    void shouldGetUserIdWhenPayloadIsNull() {
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithRandomUUID("usersgroups.get-user-details")
                .withUserId(USER_ID), createObjectBuilder().build());

        final JsonObject jsonObject = createObjectBuilder()
                .build();

        when(requester.requestAsAdmin(any(), any())).thenReturn(Envelope.envelopeFrom(requestEnvelope.metadata(), jsonObject));

        final String response = userService.getUserDetails(USER_ID);

        verify(requester).requestAsAdmin(any(), any());
        assertThat(response, is(USER_ID));
    }

}
