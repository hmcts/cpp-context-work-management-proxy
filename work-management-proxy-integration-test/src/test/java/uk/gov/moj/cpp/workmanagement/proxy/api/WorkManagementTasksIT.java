package uk.gov.moj.cpp.workmanagement.proxy.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.ReferenceDataStub.setupRefDataGetWorkQueue;
import static uk.gov.moj.cpp.workmanagement.proxy.api.helper.RestHelper.pollForResponse;

import uk.gov.moj.cpp.workmanagement.proxy.api.helper.AbstractIT;
import uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorkManagementTasksIT extends AbstractIT {

    public static final String PUT_UPDATE_TASK_VARIABLE_BODY = "{\"value\":\"%s\",\"type\":\"%s\"}";
    public static final String UNCLAIM_TASK_REQUEST_BODY = "{}";
    private static final String COMMENT_CREATE_URI = "/task/{0}/comment/create";
    private static final String CLAIM_TASK = "/task/{0}/claim";
    private static final String COMPLETE_TASK = "/task/{0}/complete";
    private static final String TASK_ID = "task-123457";
    private static final String UPDATE_TASK_VARIABLE = "/task/{0}/localVariables/{1}";
    private static final String UNCLAIM_TASK = "/task/{0}/unclaim";
    private static final String ASSIGN_USER_TASK = "/task/{0}/assignee";
    private static final String TASK_VARIABLE_NAME = "managerName1";
    private static final Object TASK_MANAGER_NAME = "Manager1";
    private static final Object STRING_TYPE = "String";
    private static final String REG_PATTERN = "[\\t\\n\\r\\s]";
    public static final String ORGANISATION_ID = "e7be77a9-9ac5-431d-a23b-2d750e441b75";

    @BeforeEach
    public void setUp() {
        setupRefDataGetWorkQueue("/stub-data/refdata-get-work-queue.json", "f8254db1-1683-483e-afb3-b87fde5a0a26");
    }

    @Test
    public void shouldGetAllTasksFromCamundaEngine() {
        pollForResponse("/task",
                "application/vnd.workmanagementproxy.get-task-list+json",
                withJsonPath("$.results", hasSize(2)),
                withJsonPath("$.results[*].assignee", containsInAnyOrder("john", "peter")),
                withJsonPath("$.results[*].id", containsInAnyOrder("task-123", "task-456"))
        );
    }

    @Test
    public void shouldGetTaskByIdFromCamundaEngine() {
        pollForResponse("/task/task-123",
                "application/vnd.workmanagementproxy.get-task+json",
                withJsonPath("$.assignee", is("john")),
                withJsonPath("$.id", is("task-123"))
        );
    }

    @Test
    public void shouldCreatesACommentForATaskFromCamundaEngine() {
        final Response response = postCommand(format(getBaseUrl() + COMMENT_CREATE_URI, TASK_ID),
                "application/vnd.workmanagementproxy.add-task-comment+json",
                String.format(WireMockStubUtils.POST_COMMENT_BODY, TASK_ID));
        assertThat(response.getStatus(), is(SC_ACCEPTED));
    }

    @Test
    public void shouldThrowExceptionWhenTryToCreatesACommentForInvalidTaskId() {
        final Response response = postCommand(format(getBaseUrl() + COMMENT_CREATE_URI, "task-11111"),
                "application/vnd.workmanagementproxy.add-task-comment+json",
                String.format(WireMockStubUtils.POST_COMMENT_BODY, TASK_ID));
        assertThat(response.getStatus(), is(SC_BAD_REQUEST)); // TODO will be changing to 404 after exception handling updated in interceptor
    }

    @Test
    public void shouldUpdateTaskVariableForATaskFromCamundaEngine() {
        final Response response = putCommand(format(getBaseUrl() + UPDATE_TASK_VARIABLE, TASK_ID, TASK_VARIABLE_NAME),
                "application/vnd.workmanagementproxy.update-task-variable+json",
                String.format(PUT_UPDATE_TASK_VARIABLE_BODY, TASK_MANAGER_NAME, STRING_TYPE));
        assertThat(response.getStatus(), is(SC_ACCEPTED));
    }

    @Test
    public void shouldGetListOfAvialableTasks() {
        pollForResponse("/custom/tasks?caseURN=b083e359-6576-4694-3498-7810e7fko231", "application/vnd.workmanagementproxy.custom-query-available-tasks-with-variables+json",
                withJsonPath("$.totalCount", not(empty()))
        );
    }

    @Test
    public void shouldGetListOfAvialableTasksWithOverdueFlag() {
        pollForResponse("/custom/tasks?caseURN=b083e359-6576-4694-3498-7810e7fko231&isOverdue=true", "application/vnd.workmanagementproxy.custom-query-available-tasks-with-variables+json",
                withJsonPath("$.totalCount", not(empty()))
        );
    }

    @Test
    public void shouldGetListOfAssigneeTasks() {
        pollForResponse("/custom/tasks?assignee=a085e359-6069-4694-8820-7810e7dfe762&caseTagTypes=&limit=20&_=505f466a-07fb-47e9-b105-550b7d21bcda",
                "application/vnd.workmanagementproxy.custom-query-assignee-tasks-with-variables+json",
                withJsonPath("$.totalCount", not(empty()))
        );
    }
}