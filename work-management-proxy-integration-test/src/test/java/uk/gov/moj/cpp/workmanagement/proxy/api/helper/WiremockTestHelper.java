package uk.gov.moj.cpp.workmanagement.proxy.api.helper;

import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.http.RequestParams;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

public class WiremockTestHelper {
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String BASE_URI = "http://" + HOST + ":8080";

    public static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Response.Status.OK);
    }

    public static void waitForStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RequestParams requestParams = requestParams(BASE_URI + resource, mediaType).build();

        poll(requestParams)
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(expectedStatus)
                );
    }
}
