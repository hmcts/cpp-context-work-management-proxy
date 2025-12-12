package uk.gov.moj.cpp.workmanagement.proxy.api.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WiremockTestHelper.waitForStubToBeReady;

import java.io.IOException;

import com.google.common.io.Resources;

public class UserGroupsServiceStub {
    private static final String USERSGROUPS_QUERY_API_QUERY_API_REST_USERSGROUPS_USERS = "/usersgroups-query-api/query/api/rest/usersgroups/users/";
    private static final String APPLICATION_VND_USERSGROUPS_USER_DETAILS_JSON = "application/vnd.usersgroups.user-details+json";

    public static void stubUserGroupsGetUserDetails(final String userId) throws IOException {
        stubPingFor("users-groups-service");
        String payload = Resources.toString(getResource("stub-data/user-and-groups-user-details.json"), defaultCharset());

        stubFor(get(urlPathMatching(USERSGROUPS_QUERY_API_QUERY_API_REST_USERSGROUPS_USERS + userId))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_VND_USERSGROUPS_USER_DETAILS_JSON)
                        .withBody(payload)));

        waitForStubToBeReady(USERSGROUPS_QUERY_API_QUERY_API_REST_USERSGROUPS_USERS, APPLICATION_VND_USERSGROUPS_USER_DETAILS_JSON);
    }
}
