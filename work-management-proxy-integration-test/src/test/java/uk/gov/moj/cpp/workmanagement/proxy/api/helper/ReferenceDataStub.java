package uk.gov.moj.cpp.workmanagement.proxy.api.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import javax.json.JsonObject;

public class ReferenceDataStub {
    public static void setupRefDataGetWorkQueue(final String resourceName, final String workQueueId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject ejectionReasonsJson = createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String responseBody = ejectionReasonsJson.toString()
                .replace("ID", workQueueId);
        final String urlPath = String.format("/referencedata-service/query/api/rest/referencedata/get-workqueue-name/%s", workQueueId);

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responseBody)));

        waitForStubToBeReady(urlPath, "application/vnd.workmanagementproxy.get-work-queue-name+json");
    }
}
