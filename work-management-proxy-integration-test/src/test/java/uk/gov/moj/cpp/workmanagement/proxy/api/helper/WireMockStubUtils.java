package uk.gov.moj.cpp.workmanagement.proxy.api.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.nio.charset.Charset.defaultCharset;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.RestHelper.HOST;

import com.github.tomakehurst.wiremock.admin.model.ListStubMappingsResult;
import java.io.InputStream;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireMockStubUtils {

    public static final String POST_COMMENT_BODY = "{\"message\":\"a task comment\",\"taskId\":\"%s\"}";
    public static final String POST_CREATE_GENERIC_TASK = "{\"displayName\":\"Checkphonecall2\",\"taskTitle\":\"review_process\",\"due\":\"2022-11-03T23:59:00.000Z\",\"assignee\":\"genericTaskUser\",\"candidateGroups\":\"generics\"}";
    public static final String CLAIM_TASK_REQUEST_BODY = "{\"userId\":\"aUserId\",\"taskId\":\"%s\"}";
    public static final String TASK_ID = "task-123457";
    public static final String STRING_TYPE = "String";
    public static final String PUT_UPDATE_TASK_VARIABLE_BODY = "{\"value\":\"%s\",\"type\":\"%s\",\"taskId\":\"%s\",\"varName\":\"%s\"}";
    public static final String UNCLAIM_TASK_REQUEST_BODY = "{\"taskId\":\"%s\"}";

    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockStubUtils.class);
    private static final String KEY = "test_send_email_process";
    private static final String TASK_VARIABLE_NAME = "managerName1";
    private static final Object TASK_MANAGER_NAME = "Manager1";
    public static final String ASSIGN_USER_TO_TASK_REQUEST_BODY = "{\"userId\":\"aUserId\",\"taskId\":\"%s\"}";
    private static final String USER_DETAILS_MEDIA_TYPE = "application/vnd.usersgroups.logged-in-user-details+json";
    private static final String USER_DETAILS_URL = "/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions";

    static {
        configureFor(HOST, 8080);
    }

    public static void setupAsAuthorisedUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        final String payload = getPayload("stub-data/user-and-group-details-with-permissions.json")
                .replace("USER_ID", userId.toString());

        stubFor(get(urlPathEqualTo(USER_DETAILS_URL))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, USER_DETAILS_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(USER_DETAILS_URL, USER_DETAILS_MEDIA_TYPE);
    }

    public static void setupAsUnauthorisedUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        final String payload = getPayload("stub-data/user-and-group-details-without-permissions.json")
                .replace("USER_ID", userId.toString());

        stubFor(get(urlPathEqualTo(USER_DETAILS_URL))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, USER_DETAILS_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(USER_DETAILS_URL, USER_DETAILS_MEDIA_TYPE);
    }

    public static void setupCamundaGetAllTasks(final String resource) {

        stubFor(get(urlPathEqualTo("/mock-engine-rest/task"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource))));

    }


    public static void setupCamundaGetTaskById(final String resource) {

        stubFor(get(urlPathEqualTo("/mock-engine-rest/task/task-123"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource))));

    }

    public static void setupCamundaClaimATask(final Status status) {
        stubFor(post(urlPathEqualTo(String.format("/mock-engine-rest/task/%s/claim", TASK_ID)))
                .withRequestBody(equalToJson(String.format(CLAIM_TASK_REQUEST_BODY, TASK_ID)))
                .willReturn(aResponse().withStatus(status.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

    }

    public static void setupCamundaCompleteATask(final String resource) {
        stubFor(post(urlPathEqualTo(String.format("/mock-engine-rest/task/%s/complete", TASK_ID)))
                .withRequestBody(equalToJson(String.format(getPayload(resource), TASK_ID)))
                .willReturn(aResponse().withStatus(NO_CONTENT.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));


        ListStubMappingsResult listStubMappingsResult = WireMock.listAllStubMappings();
        listStubMappingsResult.getMappings().stream().forEach(stubMapping -> {
            LOGGER.info("request: {}", stubMapping.getRequest());
            LOGGER.info("response: {}", stubMapping.getResponse());
            LOGGER.info("--------------------------------------------------------------------------");
        });
    }

    public static void setupCamundaPostCommentsForATask(final String resource) {
        stubFor(post(urlPathEqualTo(String.format("/mock-engine-rest/task/%s/comment/create", TASK_ID)))
                .withRequestBody(equalToJson(String.format(POST_COMMENT_BODY, TASK_ID)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource))));

    }


    public static void setupCamundaPutUpdateTaskLocalVariable(final Status status) {
        stubFor(put(urlPathEqualTo(String.format("/mock-engine-rest/task/%s/localVariables/%s", TASK_ID, TASK_VARIABLE_NAME)))
                .withRequestBody(equalToJson(String.format(PUT_UPDATE_TASK_VARIABLE_BODY,
                        TASK_MANAGER_NAME, STRING_TYPE, TASK_ID, TASK_VARIABLE_NAME)))
                .willReturn(aResponse().withStatus(status.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

    }


    public static void setupCamundaStartProcessInstance(final String request, final String response) {
        stubFor(post(urlPathEqualTo(String.format("/mock-engine-rest/process-definition/key/%s/start", KEY)))
                .withRequestBody(equalToJson(getPayload(request)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(response))));

    }


    public static void setupCamundaUnClaimATask(final Status status, final String response) {
        stubFor(post(urlPathEqualTo(String.format("/mock-engine-rest/task/%s/unclaim", TASK_ID)))
                .withRequestBody(equalToJson(String.format(UNCLAIM_TASK_REQUEST_BODY, TASK_ID)))
                .willReturn(aResponse().withStatus(status.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(response))));
    }


    public static void setupCamundaAssignUserATask(final Status status, final String response) {
        stubFor(post(urlPathEqualTo(String.format("/mock-engine-rest/task/%s/assignee", TASK_ID)))
                .withRequestBody(equalToJson(String.format(ASSIGN_USER_TO_TASK_REQUEST_BODY, TASK_ID)))
                .willReturn(aResponse().withStatus(status.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(response))));
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType, final Status expectedStatus) {
        var urlPattern = resource.startsWith("/") ? "{0}{1}" : "{0}/{1}";
        poll(requestParams(format(urlPattern, getBaseUri(), resource), mediaType).build())
                .until(status().is(expectedStatus));
    }

    public static String getPayload(final String path) {
        String request = null;
        try (final InputStream inputStream = WireMockStubUtils.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream, notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset());
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

}
