package uk.gov.moj.cpp.workmanagement.proxy.api.helper;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class RestHelper {

    public static final int TIMEOUT = 30;
    public static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    protected static final String BASE_URI = System.getProperty("baseUri", "http://" + HOST + ":8080");
    private static final String READ_BASE_URL = "/work-management-proxy-api/rest/workmanagementproxy";
    private static final int POLL_INTERVAL = 2;

    public static String getReadUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }

    public static String pollForResponse(final String path, final String mediaType) {
        return pollForResponse(path, mediaType, randomUUID().toString(), status().is(OK));
    }

    public static String pollForResponse(final String path, final String mediaType, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, randomUUID().toString(), payloadMatchers);
    }

    public static String pollForResponse(final String path, final String mediaType, final String userId, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, userId, status().is(OK), payloadMatchers);
    }


    public static String pollForResponse(final String path, final String mediaType, final String userId, final ResponseStatusMatcher responseStatusMatcher, final Matcher... payloadMatchers) {

        return poll(requestParams(getReadUrl(path), mediaType)
                .withHeader(USER_ID, userId).build())
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        responseStatusMatcher,
                        payload().isJson(allOf(payloadMatchers))
                )
                .getPayload();
    }

    public static String pollForResponse(final String path,
                                         final String mediaType,
                                         final String userId, List<Matcher<? super ReadContext>> matchers) {
        return poll(requestParams(getReadUrl(path),
                mediaType)
                .withHeader(USER_ID, userId))
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))).getPayload();

    }
}
