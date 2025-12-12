package uk.gov.moj.cpp.workmanagement.proxy.api;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.getPayload;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils.setupAsUnauthorisedUser;

import uk.gov.moj.cpp.workmanagement.proxy.api.helper.AbstractIT;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorkflowRequestPermissionIT extends AbstractIT {

    private static final String PROCESS_DEFINITION_START_URL = "/process-definition/key/%s/start";
    private static final String GET_TASK_LIST_URL = "/task";
    private static final String KEY = "test_send_email_process";

    @BeforeEach
    public void setUp() {
        setupAsUnauthorisedUser(USER_ID_VALUE);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToSendUpdatePermittedRequest() {
        final Response response = postCommand(getBaseUrl() + format(PROCESS_DEFINITION_START_URL, KEY),
                "application/vnd.workmanagementproxy.start-process+json",
                getPayload("stub-data/camunda-start-process-request-body.json"));
        assertThat(response.getStatus(), is(SC_FORBIDDEN));
    }

    @Test
    public void shouldNotAllowAuthorisedUserToSendViewPermittedRequest() {
        final Response response = query(getBaseUrl() + GET_TASK_LIST_URL, "application/vnd.workmanagementproxy.get-task-list+json");
        assertThat(response.getStatus(), is(SC_FORBIDDEN));
    }
}
