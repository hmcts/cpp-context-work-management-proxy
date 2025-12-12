package uk.gov.moj.cpp.workmanagement.proxy.api.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static java.lang.System.getProperty;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.RestHelper.HOST;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaAssignUserATask;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaClaimATask;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaCompleteATask;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaGetAllTasks;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaGetTaskById;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaPostCommentsForATask;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaPutUpdateTaskLocalVariable;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaStartProcessInstance;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupCamundaUnClaimATask;

import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;

public class AbstractIT {

    protected static final Logger LOGGER = getLogger(AbstractIT.class);
    protected static final RestClientService restClient = new RestClientService();
    protected static final String BASE_URI = getProperty("baseUri", "http://" + HOST + ":8080");
    protected static final String CAMUNDA_WORKFLOW_URI = "/work-management-proxy-api/rest/workmanagementproxy";
    protected static final UUID USER_ID_VALUE = randomUUID();

    /**
     * NOTE: this approach is employed to enabled massive savings in test execution test.
     * All tests will need to extend AbstractIT thus ensuring the static initialisation block is fired just once before any test runs
     * Mock reset and stub for all reference data happens once per VM.  If parallel test run is considered, this approach will be tweaked.
     */

    @BeforeAll
    public static void beforeClass() {
        configureFor(RestHelper.HOST, 8080);
        reset(); // will need to be removed when things are being run in parallel
        LOGGER.info("stubs configuration host:{}", RestHelper.HOST);
        defaultStubs();
    }

    private static void defaultStubs() {
        setupAsAuthorisedUser(USER_ID_VALUE);
        setupCamundaGetAllTasks("stub-data/camunda-get-tasks-response.json");
        setupCamundaGetTaskById("stub-data/camunda-get-task-by-id-response.json");
        setupCamundaPostCommentsForATask("stub-data/camunda-add-task-comment.json");
        setupCamundaStartProcessInstance("stub-data/camunda-start-process-request-body.json", "stub-data/camunda-start-process-response-body.json");
        setupCamundaClaimATask(NO_CONTENT);
        setupCamundaPutUpdateTaskLocalVariable(NO_CONTENT);
        setupCamundaCompleteATask("stub-data/camunda-complete-task-request-body.json");
        setupCamundaUnClaimATask(NO_CONTENT, "stub-data/camunda-unclaim-valid-task.json");
        setupCamundaAssignUserATask(NO_CONTENT, "stub-data/camunda-unclaim-valid-task.json");
        LOGGER.info("stubs completed !!!");
    }

    public static String getBaseUrl() {
        return BASE_URI + CAMUNDA_WORKFLOW_URI;
    }

    protected Response postCommand(final String url, final String contentType, final String requestPayload) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, USER_ID_VALUE);

        LOGGER.info("BASE_URI: {}, restClient {}", BASE_URI, restClient);
        return restClient.postCommand(url, contentType, requestPayload, headers);
    }

    protected Response putCommand(final String url, final String contentType, final String requestPayload) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, USER_ID_VALUE);

        LOGGER.info("BASE_URI: {}, restClient {}", BASE_URI, restClient);
        return restClient.putCommand(url, contentType, requestPayload, headers);
    }

    protected Response query(final String url, final String contentType) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, USER_ID_VALUE);

        LOGGER.info("BASE_URI: {}, restClient {}", BASE_URI, restClient);
        return restClient.query(url, contentType, headers);
    }
}