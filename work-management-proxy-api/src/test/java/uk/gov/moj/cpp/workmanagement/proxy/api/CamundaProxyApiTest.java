package uk.gov.moj.cpp.workmanagement.proxy.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.util.Date.from;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANDIDATE_GROUPS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COMMENT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DUE_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_DUE_SOON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LIMIT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.OFFSET;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_TYPE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_TYPE_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE_TYPE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE_TYPES;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils.convertToDate;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.FilteredTasks;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowTaskType;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.CamundaJavaApiService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.ReferenceDataService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.ResponseEnrichmentService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskFilterService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.UserService;
import uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, ProcessEngineExtension.class})
@Deployment(resources = {"sample-test-process.bpmn","sample-test-process-2.bpmn", "custom-task-process.bpmn"})
public class CamundaProxyApiTest {

    /*This is sample process deployed to run the test cases. process file is present in the resources folder
     * Should file be changed then below process specific variables should be replaced with new uploaded process key */
    private static final String PROCESS_KEY_VALUE = "sample_test_custom_task_process";
    private static final String CUSTOM_PROCESS_KEY_VALUE = "single_custom_task_process";
    private static final String CUSTODY_TIME_LIMIT = "custodyTimeLimit";
    private static final String IS_URGENT = "isUrgent";
    private static final String CASE_URN = "caseURN";
    private static final String HEARING_DATE = "hearingDate";
    private static final String HEARING_TYPE = "hearingType";
    private static final String ASSIGNEE = "assignee";
    private static final String ACTIVITY_TYPES = "activityTypes";
    private static final String CUSTOM_TASK_TYPE = "customTaskType";
    private static final String IS_CTL_ONLY = "isCtlOnly";
    private static final String CANDIDATE_GROUP = "candidateGroups";
    private static final String REGION = "region";
    public static final String ORGANISATION_ID = "organisationId";

    private static final String TASK_VARIABLES_JSON_STRING = "taskVariablesJsonString";
    private static final String JSON_TASK_DETAIL = "json/taskDetail.json";

    private static final String CANCELLATION_REASON = "cancellationReason";
    private static String taskId = "11";
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final String DUE_DATE_STRING = "2022-12-12T00:00:00.000Z";
    private static final String WRONG_DUE_DATE_STRING = "2022-12-1200:00:00.000Z";
    private static final String DUE_DATE_STRING_PARSE_EXCEPTION = "Date format must be: yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final String RESPONSE_DEEP_LINK = "deepLink";
    private static final String RESPONSE_WORK_QUEUE = "WORK_QUEUE";
    private static final String USER_ID = "userId";
    private static final String TASK_ID = "taskId";

    private static final String IS_OVERDUE = "isOverdue";

    private static final UUID organisationIdUuid = randomUUID();
    private static final String organisationIdStr = organisationIdUuid.toString();

    private static final String TASK_STATUS = "taskStatus";

    private static final String TASK_ONE_NAME_VAL = "First Task";
    private static final String TASK_SECOND_NAME_VAL = "Second Task";
    private static final String TASK_ONE_CUSTOM_TASK_TYPE_VAL = "firstTask";
    private static final String CASE_URN_VAL = "TEST_CPS_CASE_001";
    private static final String HEARING_TYPE_VAL = "hearing_type_1";
    private static final String REGION_HMCTS_VAL = "HMCTS";
    private static final String REGION_CPS_VAL = "CPS";
    private static final String CANDIDATE_GROUP_HMCTS_VAL = "hmcts_group";
    private static final String CANDIDATE_GROUP_RANDOM_VAL = "random_group";
    private static final String TASK_ONE_ACTIVITY_TYPE_VAL = "firstTask";
    private static final String TASK_SECOND_ACTIVITY_TYPE_VAL = "secondTask";
    private static final String TASK_ONE_COMMENT_VAL = "task1 comment";
    private static final String TASK_ONE_TYPE_VAL = "taskType1";
    private static final UUID TASK_ONE_TYPE_ID_VAL = randomUUID();
    private static final String TASK_ONE_ASSIGNEE_VAL = "testAssignee1";
    private static final String CASE_ID_VAL = "e7be77a9-9ac5-431d-a23b-2d750e441b75";
    private static final String USER_ID_ONE_VAL = "testAssignee1";
    private static final String REASON_VAL = "reopen task";

    private static final String WORKQUEUE_VAL = "e7be77a9-9ac5-431d-a23b-2d750e441b76";

    private static final String TASK_VARIABLE_JSON_INPUT_VALUE = "{\"displayName\":\"First Task\",\"taskName\":\"customTask\",\"due\":\"2024-06-27T23:59:00.000Z\",\"caseURN\":\"29GD7875621\"," +
            "\"assignee\":\"customTaskAssignee\",\"candidateGroups\":\"3d1b2be0-f92a-4291-9b99-17af7e645904,6b1b2be0-f92a-4291-9b99-17af7e645123\",\"isDeletable\":true,\"isDeferrable\":false," +
            "\"organisationId\":\"7f2b2be0-f92a-4291-9b99-17af7e645321\",\"caseId\":\"8e4b2be0-f92a-4291-9b99-17af7e645472\",\"businessUnitCodes\":\"businessUnitOuCode\",\"courtCodes\":\"courtOuCode\",\"isUrgent\":false}";

    private CompleteTaskApi deleteTaskApi = new CompleteTaskApi();

    @RegisterExtension
    ProcessEngineExtension extension = ProcessEngineExtension.builder()
            .configurationResource("camunda.cfg.xml")
            .build();

    private ProcessInstance processInstance;

    private final CamundaProxyApi camundaProxyApi = new CamundaProxyApi();

    private final CamundaJavaApiService camundaJavaApiService = new CamundaJavaApiService();

    private final TaskFilterService taskFilterService = new TaskFilterService();

    private final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

    private final ResponseEnrichmentService responseEnrichmentService = new ResponseEnrichmentService();

    @Mock
    private UserService userService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private VariableInstance variableInstance;

    private String userDetails = "Jane Doe";

    private TaskService taskService;
    private RuntimeService runtimeService;

    private HistoryService historyService;
    protected ProcessEngine processEngine;

    @Spy
    private ObjectMapperProducer objectMapperProducer;

    @BeforeEach
    void setUp() {
        taskService = extension.getProcessEngine().getTaskService();
        runtimeService = extension.getProcessEngine().getRuntimeService();
        historyService = extension.getProcessEngine().getHistoryService();
        processEngine = extension.getProcessEngine();
        setField(taskFilterService, "taskService", taskService);
        setField(taskFilterService, "historyService", historyService);
        setField(taskFilterService, "runtimeService", runtimeService);
        setField(camundaJavaApiService, "taskService", taskService);
        setField(camundaJavaApiService, "runtimeService", runtimeService);
        setField(camundaJavaApiService, "taskFilterService", taskFilterService);
        setField(camundaJavaApiService, "userService", userService);
        setField(camundaJavaApiService, "referenceDataService", referenceDataService);
        setField(camundaJavaApiService, "historyService", historyService);
        setField(camundaJavaApiService, "objectMapperProducer", objectMapperProducer);
        setField(camundaProxyApi, "camundaJavaApiService", camundaJavaApiService);
        setField(camundaProxyApi, "responseEnrichmentService", responseEnrichmentService);
        setField(deleteTaskApi, "taskService", taskService);
        setField(deleteTaskApi, "userService", userService);
        setField(deleteTaskApi, "runtimeService", runtimeService);
        setField(deleteTaskApi, "processEngine", processEngine);
    }

    @Test
    void testGetCustomQueryAssigneeTasksWithVariablesNoFilters() {

        createProcessInstanceWithTaskAndVariables(true, true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee1")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("organisationId", organisationIdStr)
                .add(ASSIGNEE, "testAssignee1")
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAssigneeTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].assignee", is("testAssignee1")),
                withJsonPath("$.tasks[0].name", is(TASK_ONE_NAME_VAL)),
                withJsonPath("$.tasks[0].due", is(notNullValue())),
                withJsonPath("$.tasks[0].custodyTimeLimit", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingType", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingDate", is(notNullValue())),
                withJsonPath("$.tasks[0].isUrgent", is(true))
        )));
    }

    @Test
    public void testGetTaskDetailsWhenTaskIdExist() throws InterruptedException {
        createProcessInstanceWithTaskAndVariables(true, false);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee1")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("taskId", taskId)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getTaskDetails(jsonEnvelope);
        final JsonObject taskDetailResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(taskDetailResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.task.name", is(TASK_ONE_NAME_VAL)))));
    }

    @Test
    public void testGetTaskDetailsWhenTaskIdNotExist() throws InterruptedException {
        createProcessInstanceWithTaskAndVariables(true, false);

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("taskId", "")
                .build());

        try {
            camundaProxyApi.getTaskDetails(jsonEnvelope);
        } catch (Exception e) {
            assertThat(e.toString(), e instanceof BadRequestException);
        }

    }

    @Test
    public void testGetTaskDetailsWhenTaskIdExistWithOutHearingDate() throws InterruptedException {
        createProcessInstanceWithTaskAndVariables(false, false);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee1")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("taskId", taskId)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getTaskDetails(jsonEnvelope);
        final JsonObject taskDetailResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();

        assertThat(taskDetailResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.task.name", is(TASK_ONE_NAME_VAL)))));
    }

    @Test
    void testGetCustomQueryAssigneeTasksWithVariablesAllFilters() {
        createProcessInstanceWithTaskAndVariables(true, false);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("organisationId", organisationIdStr)
                .add(ASSIGNEE, "testAssignee2")
                .add(ACTIVITY_TYPES,  "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(TASK_STATUS, "active")
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAssigneeTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].assignee", is("testAssignee2")),
                withJsonPath("$.tasks[0].commentCount", is(0)),
                withJsonPath("$.tasks[0].name", is(TASK_SECOND_NAME_VAL)),
                withJsonPath("$.tasks[0].due", is(notNullValue())),
                withJsonPath("$.tasks[0].custodyTimeLimit", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingType", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingDate", is(notNullValue())),
                withJsonPath("$.tasks[0].isUrgent", is(true))
        )));
    }

    @Test
    void testGetCustomQueryAssigneeTasksWithVariablesAllFiltersWithOutGHearingDate() {
        createProcessInstanceWithTaskAndVariables(false, false);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("organisationId", organisationIdStr)
                .add(ASSIGNEE, "testAssignee2")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAssigneeTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].assignee", is("testAssignee2")),
                withJsonPath("$.tasks[0].commentCount", is(0)),
                withJsonPath("$.tasks[0].name", is(TASK_SECOND_NAME_VAL)),
                withJsonPath("$.tasks[0].due", is(notNullValue())),
                withJsonPath("$.tasks[0].custodyTimeLimit", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingType", is(notNullValue())),
                withJsonPath("$.tasks[0].isUrgent", is(true))
        )));
    }

    @Test
    void testGetCustomQueryAssigneeTasksWithVariablesAllFiltersAndPagination() {
        for (int i = 0; i < 50; i++) {
            createProcessInstanceWithTaskAndVariables(true, false);
        }

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(ASSIGNEE, "testAssignee2")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAssigneeTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(50)),
                withJsonPath("$.tasks", hasSize(20))
        )));

        final JsonArray tasksJsonArray = customQueryResponseAsJsonObject.getJsonArray("tasks");
        for (int j = 0; j < 20; j++) {
            JsonObject taskJsonObject = tasksJsonArray.getJsonObject(j);
            assertThat(taskJsonObject.toString(), isJson(allOf(
                    withJsonPath("$.assignee", is("testAssignee2")),
                    withJsonPath("$.name", is(TASK_SECOND_NAME_VAL)),
                    withJsonPath("$.due", is(notNullValue())),
                    withJsonPath("$.custodyTimeLimit", is(notNullValue())),
                    withJsonPath("$.hearingType", is(notNullValue())),
                    withJsonPath("$.hearingDate", is(notNullValue())),
                    withJsonPath("$.isUrgent", is(true))
//                    withJsonPath("$.candidateGroups", containsInAnyOrder("cps_group", "random_cps_group"))
            )));
        }
    }

    @Test
    void testGetCustomQueryAvailableTasksWithVariablesNoFilters() {
        createProcessInstanceWithUnassignedTaskAndVariables(true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee1")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(REGION, REGION_HMCTS_VAL)
                .add(ACTIVITY_TYPES, TASK_ONE_ACTIVITY_TYPE_VAL)
                .add(WORK_QUEUE_TYPE, WORKQUEUE_VAL)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAvailableTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].assignee", isEmptyString()),
                withJsonPath("$.tasks[0].name", is(TASK_ONE_NAME_VAL)),
                withJsonPath("$.tasks[0].due", is(notNullValue())),
                withJsonPath("$.tasks[0].custodyTimeLimit", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingType", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingDate", is(notNullValue())),
                withJsonPath("$.tasks[0].isUrgent", is(true)))
        ));
    }

    @Test
    void testGetCustomQueryAvailableTasksWithVariablesAllFilters() {
        createProcessInstanceWithUnassignedTaskAndVariables(true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(REGION, REGION_HMCTS_VAL)
                .add(ACTIVITY_TYPES, TASK_ONE_ACTIVITY_TYPE_VAL)
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(WORK_QUEUE_TYPE, WORKQUEUE_VAL)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAvailableTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].assignee", isEmptyString()),
                withJsonPath("$.tasks[0].name", is(TASK_ONE_NAME_VAL)),
                withJsonPath("$.tasks[0].due", is(notNullValue())),
                withJsonPath("$.tasks[0].custodyTimeLimit", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingType", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingDate", is(notNullValue())),
                withJsonPath("$.tasks[0].isUrgent", is(true))
        )));
    }

    @Test
    void testGetCustomQueryAvailableTasksWithActivityType() {
        createProcessInstanceWithUnassignedTaskAndVariables(true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(ACTIVITY_TYPES, TASK_ONE_ACTIVITY_TYPE_VAL)
                .add(WORK_QUEUE_TYPES, WORKQUEUE_VAL)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAvailableTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].assignee", isEmptyString()),
                withJsonPath("$.tasks[0].name", is(TASK_ONE_NAME_VAL)),
                withJsonPath("$.tasks[0].due", is(notNullValue())),
                withJsonPath("$.tasks[0].custodyTimeLimit", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingType", is(notNullValue())),
                withJsonPath("$.tasks[0].hearingDate", is(notNullValue())),
                withJsonPath("$.tasks[0].isUrgent", is(true))
        )));
    }

    @Test
    void testGetCustomQueryAvailableTasksWithVariablesAllFiltersAndPagination() {

        for (int i = 0; i < 50; i++) {
            createProcessInstanceWithUnassignedTaskAndVariables(true);
        }

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(REGION, REGION_HMCTS_VAL)
                .add(ACTIVITY_TYPES, TASK_ONE_ACTIVITY_TYPE_VAL)
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(WORK_QUEUE_TYPES, WORKQUEUE_VAL)
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAvailableTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(50)),
                withJsonPath("$.tasks", hasSize(20))
        )));

        final JsonArray tasksJsonArray = customQueryResponseAsJsonObject.getJsonArray("tasks");
        for (int j = 0; j < 20; j++) {
            JsonObject taskJsonObject = tasksJsonArray.getJsonObject(j);
            assertThat(taskJsonObject.toString(), isJson(allOf(
                    withJsonPath("$.assignee", isEmptyString()),
                    withJsonPath("$.name", is(TASK_ONE_NAME_VAL)),
                    withJsonPath("$.due", is(notNullValue())),
                    withJsonPath("$.custodyTimeLimit", is(notNullValue())),
                    withJsonPath("$.hearingType", is(notNullValue())),
                    withJsonPath("$.hearingDate", is(notNullValue())),
                    withJsonPath("$.isUrgent", is(true))
//                    withJsonPath("$.candidateGroups", containsInAnyOrder("hmcts_group","random_group"))
            )));
        }
    }

    @Test
    public void testGetActivitiesSummary() {

        createProcessInstanceWithUnassignedTaskAndVariables(true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();

        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getActivitySummary(jsonEnvelope);
        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject();

        assertThat(jsonObject.getInt("totalActivities"), is(2));

        final JsonArray tasksArray = jsonObject.getJsonArray("activities");
        assertThat(tasksArray, hasSize(2));

        final JsonObject task1 = tasksArray.getJsonObject(0);
        assertThat(task1.getString("taskName"), is("firstTask"));
        assertThat(task1.getJsonObject("activityStat"), is(notNullValue()));

        final JsonObject task2 = tasksArray.getJsonObject(1);
        assertThat(task2.getString("taskName"), is("secondTask"));
        assertThat(task1.getJsonObject("activityStat"), is(notNullValue()));
    }

    @Test
    public void testGetActivitiesSummaryForMultipleTasks() {

        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithUnassignedTaskAndVariables(true);
        }

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();

        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(OFFSET, 0)
                .add(LIMIT, 100)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getActivitySummary(jsonEnvelope);
        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject();

        assertThat(jsonObject.getInt("totalActivities"), is(2));

        final JsonArray tasksArray = jsonObject.getJsonArray("activities");
        assertThat(tasksArray, hasSize(2));

        final JsonObject task1 = tasksArray.getJsonObject(0);
        assertThat(task1.getString("taskName"), is("firstTask"));
        assertThat(task1.getJsonObject("activityStat"), is(notNullValue()));

        final JsonObject task2 = tasksArray.getJsonObject(1);
        assertThat(task2.getString("taskName"), is("secondTask"));
        assertThat(task1.getJsonObject("activityStat"), is(notNullValue()));
    }

    @Test
    void shouldCreateCustomTaskAndVariables() {
        final String assignee = randomUUID().toString();
        processInstance = runtimeService.startProcessInstanceByKey(CUSTOM_PROCESS_KEY_VALUE);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee1")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("organisationId", organisationIdStr)
                .add(TASK_NAME, "sample_test")
                .add(TASK_TYPE, "task type")
                .add(COMMENT, "COMMENT")
                .add(ASSIGNEE, assignee)
                .add(HEARING_DATE, "2024-08-12")
                .build());
        final JsonEnvelope responseEnvelope = camundaProxyApi.createCustomTaskAndVariables(jsonEnvelope);
        final JsonObject taskDetailResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();

        assertThat(taskDetailResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.task.name", is("First Task")),
                withJsonPath("$.task.assignee", is(assignee)),
                withJsonPath("$.task.lastUpdatedByID", is("testAssignee1")),
                withJsonPath("$.task.hearingDate", is("2024-08-12")))));

    }

    @Test
    void shouldUpdateDueDate() {
        createProcessInstanceWithUnassignedTaskAndVariables(true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(REGION, REGION_HMCTS_VAL)
                .add(ACTIVITY_TYPES, TASK_ONE_ACTIVITY_TYPE_VAL)
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(WORK_QUEUE_TYPE, WORKQUEUE_VAL)
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        TaskWithVariables originalTask = tasksWithVariables.get(0);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(TASK_ID, originalTask.getTask().getId())
                .add(ContextConstants.DUE_DATE, DUE_DATE_STRING).build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.updateTask(jsonEnvelope);
        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(jsonObject.toString(), isJson(allOf(
                withJsonPath("$.task.due", is(DUE_DATE_STRING))
        )));

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(TASK_ID, originalTask.getTask().getId()).build());

        final JsonEnvelope responseEnvelope1 = camundaProxyApi.updateTask(jsonEnvelope);
        final JsonObject jsonObject1 = responseEnvelope1.payloadAsJsonObject();
        assertThat(jsonObject1.toString(), isJson(allOf(
                withJsonPath("$.task.due", is(DUE_DATE_STRING)))));
    }

    @Test
    void shouldAssignTask() {
        createProcessInstanceWithUnassignedTaskAndVariables(true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(REGION, REGION_HMCTS_VAL)
                .add(ACTIVITY_TYPES, "firstTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(WORK_QUEUE_TYPES, WORKQUEUE_VAL)
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        TaskWithVariables originalTask = tasksWithVariables.get(0);
        taskService.setAssignee(originalTask.getTask().getId(), "User");
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(USER_ID, "Test")
                .add(TASK_ID, originalTask.getTask().getId())
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.assignTask(jsonEnvelope);
        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(jsonObject.toString(), isJson(allOf(
                withJsonPath("$.task.assignee", is("Test"))
        )));
    }

    @Test
    void shouldReOpenTask() throws JsonProcessingException, ParseException {
        createProcessInstanceWithTaskAndVariables(true, true);
        cancelTask(taskId);

        when(userService.getUserDetails(any())).thenReturn(userDetails);
        when(referenceDataService.getWorkFlowTaskTypeWithId(any()))
                .thenReturn(WorkflowTaskType.builder().taskName("sample_test_2").isCustomTask(true).build());

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId(USER_ID_ONE_VAL)
                .build());
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(TASK_ID, taskId)
                .add(ASSIGNEE, USER_ID_ONE_VAL)
                .add(DUE_DATE, DUE_DATE_STRING)
                .add(REASON, REASON_VAL)
                .add(CANDIDATE_GROUPS, CANDIDATE_GROUP_HMCTS_VAL)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.reopenTask(jsonEnvelope);

        assertNotNull(responseEnvelope);

        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(jsonObject.toString(), isJson(allOf(
                withJsonPath("$.task.name", is("Fourth Task")),
                withJsonPath("$.task.assignee", is(USER_ID_ONE_VAL)),
                withJsonPath("$.task.lastUpdatedByID", is(TASK_ONE_ASSIGNEE_VAL))
        )));
    }

    @Test
    void shouldUpdateTaskVariable() {
        createProcessInstanceWithUnassignedTaskAndVariables(true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, organisationIdStr)
                .add(REGION, REGION_HMCTS_VAL)
                .add(ACTIVITY_TYPES, "firstTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(WORK_QUEUE_TYPES, WORKQUEUE_VAL)
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        TaskWithVariables originalTask = tasksWithVariables.get(0);
        taskService.setAssignee(originalTask.getTask().getId(), "User");
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(USER_ID, "Test")
                .add("varName", WORK_QUEUE)
                .add("value", UUID.randomUUID().toString())
                .add(TASK_ID, originalTask.getTask().getId())
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.updateTaskVariable(jsonEnvelope);
        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(jsonObject.toString(), isJson(allOf(
                withJsonPath("$.task.assignee", is("User"))
        )));

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(USER_ID, "Test")
                .add("varName", "AnotherVar")
                .add("value", UUID.randomUUID().toString())
                .add(TASK_ID, originalTask.getTask().getId())
                .build());

        final JsonEnvelope responseEnvelope1 = camundaProxyApi.updateTaskVariable(jsonEnvelope);
        final JsonObject jsonObject1 = responseEnvelope1.payloadAsJsonObject();
        assertThat(jsonObject1.toString(), isJson(allOf(
                withJsonPath("$.task.assignee", is("User"))
        )));

    }

    @Test
    void shouldUpdateTaskVariableThrowsException() {
        createProcessInstanceWithUnassignedTaskAndVariables(true);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();

        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        when(envelope.metadata()).thenReturn(metadataBuilder
                .build());

        when(envelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("varName", "AnotherVar")
                .add("value", UUID.randomUUID().toString())
                .build());

        assertThrows(IllegalArgumentException.class, () -> camundaProxyApi.updateTaskVariable(envelope));

    }

    @Test
    public void testGetCustomQueryAvailableTasksWithOverdue() {
        createProcessInstanceWithOverdueFlag(true);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(IS_OVERDUE, true)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAvailableTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        String today = now().format(ISO_DATE);

        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].due", lessThan(today))
        )));
    }

    @Test
    public void testGetCustomQueryAvailableTasksWithDueSoon() {
        createProcessInstanceWithDueSoonFlag(true);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(IS_DUE_SOON, true)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAvailableTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        String today = now().format(ISO_DATE);

        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].due", greaterThan(today)),
                withJsonPath("$.tasks[0].name", is("Second Task"))
        )));
    }

    @Test
    public void testGetCustomQueryAvailableTasksWithDueSoon2() {
        createProcessInstanceWithDueSoonFlag2(true);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(IS_DUE_SOON, true)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAvailableTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        String today = now().format(ISO_DATE);

        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(1)),
                withJsonPath("$.tasks", hasSize(1)),
                withJsonPath("$.tasks[0].due", greaterThan(today)),
                withJsonPath("$.tasks[0].name", is("First Task"))
        )));
    }

    @Test
    public void testGetCustomQueryAvailableTasksWithoutOverdue() {
        createProcessInstanceWithOverdueFlag(false);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(IS_OVERDUE, false)
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAvailableTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                withJsonPath("$.totalCount", is(2)),
                withJsonPath("$.tasks", hasSize(2)),
                withJsonPath("$.tasks[0].due", notNullValue()),
                withJsonPath("$.tasks[1].due", notNullValue())
        )));
    }

    private List<Task> getTasks() {
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, organisationIdStr);
        runtimeService.setVariable(processInstanceId, CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        runtimeService.setVariable(processInstanceId, REGION, REGION_HMCTS_VAL);
        runtimeService.setVariable(processInstanceId, TASK_NAME, "firstTask");
        return taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
    }

    private void createProcessInstanceWithUnassignedTaskAndVariables(final boolean isHearingDateExist) {
        Date dueDate1 = convertToDate("2022-05-23T10:00:00.000Z");
        Date dueDate2 = convertToDate("2022-05-23T11:00:00.000Z");
        Date hearingDate = dueDate1;

        final List<Task> activeTasks = getTasks();

        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);
        taskOne.setDueDate(dueDate1);
        taskService.saveTask(taskOne);
        taskTwo.setDueDate(dueDate2);
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is(TASK_ONE_NAME_VAL));
        assertThat(taskTwo.getName(), is(TASK_SECOND_NAME_VAL));

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CUSTOM_TASK_TYPE, TASK_ONE_ACTIVITY_TYPE_VAL);
        taskOneVariables.put(CASE_URN, CASE_URN_VAL);
        if (isHearingDateExist) {
            taskOneVariables.put(HEARING_DATE, hearingDate);
        }
        taskOneVariables.put(HEARING_TYPE, HEARING_TYPE_VAL);
        taskOneVariables.put(REGION, REGION_HMCTS_VAL);
        taskOneVariables.put(ORGANISATION_ID, organisationIdStr);
        taskOneVariables.put(WORK_QUEUE, WORKQUEUE_VAL);
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), CANDIDATE_GROUP_HMCTS_VAL);
        taskService.addCandidateGroup(taskOne.getId(), CANDIDATE_GROUP_RANDOM_VAL);

        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, CASE_URN_VAL);
        taskTwoVariables.put(ACTIVITY_TYPES, TASK_SECOND_ACTIVITY_TYPE_VAL);
        if (isHearingDateExist) {
            taskTwoVariables.put(HEARING_DATE, hearingDate);
        }
        taskTwoVariables.put(HEARING_TYPE, HEARING_TYPE_VAL);
        taskTwoVariables.put(REGION, REGION_CPS_VAL);
        taskTwoVariables.put(ORGANISATION_ID, organisationIdStr);
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");
        taskService.addCandidateGroup(taskTwo.getId(), "random_cps_group");
    }

    private void createProcessInstanceWithTaskAndVariables(final boolean isHearingDateExist, boolean hasTaskComment) {
        Date dueDate = convertToDate("2022-05-23T10:00:00.000Z");
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, organisationIdStr);
        runtimeService.setVariable(processInstanceId, CASE_URN, CASE_URN_VAL);
        runtimeService.setVariable(processInstanceId, CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        runtimeService.setVariable(processInstanceId, TASK_NAME, "firstTask");

        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        final Task taskOne = activeTasks.get(0);
        taskId = taskOne.getId();
        final Task taskTwo = activeTasks.get(1);

        if (hasTaskComment) {
            taskService.createComment(taskId, processInstanceId, TASK_ONE_COMMENT_VAL);
        }

        taskOne.setDueDate(dueDate);
        taskService.saveTask(taskOne);

        taskTwo.setDueDate(dueDate);
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is(TASK_ONE_NAME_VAL));
        assertThat(taskTwo.getName(), is(TASK_SECOND_NAME_VAL));

        taskService.setAssignee(taskOne.getId(), TASK_ONE_ASSIGNEE_VAL);

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, CASE_URN_VAL);
        taskOneVariables.put(CUSTOM_TASK_TYPE, "activity");
        if (isHearingDateExist) {
            taskOneVariables.put(HEARING_DATE, dueDate);
        }
        taskOneVariables.put(HEARING_TYPE, HEARING_TYPE_VAL);
        taskOneVariables.put(REGION, CANDIDATE_GROUP_HMCTS_VAL);
        taskOneVariables.put(CASE_ID, CASE_ID_VAL);
        taskOneVariables.put(TASK_TYPE, TASK_ONE_TYPE_VAL);
        taskOneVariables.put(TASK_TYPE_ID, TASK_ONE_TYPE_ID_VAL);
        taskOneVariables.put(ACTIVITY_TYPES, TASK_ONE_CUSTOM_TASK_TYPE_VAL);
        taskOneVariables.put(ORGANISATION_ID, organisationIdStr);
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(TASK_VARIABLES_JSON_STRING, TASK_VARIABLE_JSON_INPUT_VALUE);
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), CANDIDATE_GROUP_HMCTS_VAL);
        taskService.addCandidateGroup(taskOne.getId(), "random_hmcts_group");

        taskService.setAssignee(taskTwo.getId(), "testAssignee2");
        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, CASE_URN_VAL);
        if (isHearingDateExist) {
            taskTwoVariables.put(HEARING_DATE, dueDate);
        }
        taskTwoVariables.put(HEARING_TYPE, HEARING_TYPE_VAL);
        taskTwoVariables.put(REGION, REGION_CPS_VAL);
        taskTwoVariables.put(ACTIVITY_TYPES, TASK_SECOND_ACTIVITY_TYPE_VAL);
        taskOneVariables.put(CASE_ID, organisationIdStr);
        taskOneVariables.put(TASK_TYPE, "taskType2");
        taskOneVariables.put(WORK_QUEUE, organisationIdStr);
        taskTwoVariables.put(ORGANISATION_ID, organisationIdStr);
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");
        taskService.addCandidateGroup(taskTwo.getId(), "random_cps_group");
    }

    private void createProcessInstanceWithOverdueFlag(boolean hasOverdue) {
        final List<Task> activeTasks = getTasks();
        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);

        if (TRUE.equals(hasOverdue)) {
            taskOne.setDueDate(from(now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskTwo.setDueDate(from(now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskService.saveTask(taskOne);
            taskService.saveTask(taskTwo);
        } else {
            taskOne.setDueDate(from(now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskTwo.setDueDate(from(now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskService.saveTask(taskOne);
            taskService.saveTask(taskTwo);
        }

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        assertThat(taskTwo.getName(), is("Second Task"));

        taskService.setAssignee(taskOne.getId(), "testAssignee1");
        taskService.setAssignee(taskTwo.getId(), "testAssignee1");
    }

    private void createProcessInstanceWithDueSoonFlag(boolean isDueSoon) {
        final List<Task> activeTasks = getTasks();
        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);

        if (TRUE.equals(isDueSoon)) {
            taskOne.setDueDate(from(now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskTwo.setDueDate(from(now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskService.saveTask(taskOne);
            taskService.saveTask(taskTwo);
        } else {
            taskOne.setDueDate(from(now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskTwo.setDueDate(from(now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskService.saveTask(taskOne);
            taskService.saveTask(taskTwo);
        }

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        assertThat(taskTwo.getName(), is("Second Task"));

        taskService.setAssignee(taskOne.getId(), "testAssignee1");
        taskService.setAssignee(taskTwo.getId(), "testAssignee1");
    }

    private void createProcessInstanceWithDueSoonFlag2(boolean isDueSoon) {
        final List<Task> activeTasks = getTasks();
        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);

        if (TRUE.equals(isDueSoon)) {
            taskOne.setDueDate(from(now().plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskTwo.setDueDate(from(now().plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskService.saveTask(taskOne);
            taskService.saveTask(taskTwo);
        } else {
            taskOne.setDueDate(from(now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskTwo.setDueDate(from(now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskService.saveTask(taskOne);
            taskService.saveTask(taskTwo);
        }

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        assertThat(taskTwo.getName(), is("Second Task"));

        taskService.setAssignee(taskOne.getId(), "testAssignee1");
        taskService.setAssignee(taskTwo.getId(), "testAssignee1");
    }

    private void cancelTask(final String taskId) {
        taskService.setVariableLocal(taskId, CANCELLATION_REASON, "no longer needed");
        taskService.complete(taskId);
    }

    @Test
    public void testGetCustomQueryAssigneeTasksWithVariablesWithTaskStatus() {

        createProcessInstanceWithTaskAndVariablesWithTaskStatus(true, true);
        JsonObject jsonObject = createObjectBuilder()
                .add(RESPONSE_DEEP_LINK, "link")
                .add(RESPONSE_WORK_QUEUE, "PCWorkQueue").build();

        String workQueueName = "Court Admin";

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee1")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("organisationId", organisationIdStr)
                .add(ASSIGNEE, "testAssignee1")
                .add(TASK_STATUS, "completed")
                .build());

        final JsonEnvelope responseEnvelope = camundaProxyApi.getCustomQueryAssigneeTasksWithVariables(jsonEnvelope);
        final JsonObject customQueryResponseAsJsonObject = responseEnvelope.payloadAsJsonObject();
        assertThat(customQueryResponseAsJsonObject.toString(), isJson(allOf(
                        withJsonPath("$.totalCount", is(1)),
                        withJsonPath("$.tasks", hasSize(1)),
                        withJsonPath("$.tasks[0].assignee", is("testAssignee1")),
                        withJsonPath("$.tasks[0].status", is("completed"))
                )
        ));
    }

    @Test
    void testAddResponseVariables() {
        CamundaProxyApi spyCamundaProxyApi = spy(new CamundaProxyApi());
        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        Map<String, Object> variables = Map.of(
                "booleanVar", true,
                "intVar", 17,
                "longVar", 21L,
                "doubleVar", 54.54,
                "bigDecimalVar", new BigDecimal("65.65"),
                "bigIntegerVar", new BigInteger("78"),
                "stringVar", "stringValue"
        );

        doNothing().when(spyCamundaProxyApi).addResponseVariablesToJsonBuilder(anyString(), any(), eq(jsonObjectBuilder));

        spyCamundaProxyApi.addResponseVariables(variables, jsonObjectBuilder);

        variables.forEach((key, value) ->
                verify(spyCamundaProxyApi).addResponseVariablesToJsonBuilder(key, value, jsonObjectBuilder)
        );
    }

    @Test
    void testAddResponseVariablesToJsonBuilder() {
        CamundaProxyApi spyCamundaProxyApi = spy(new CamundaProxyApi());

        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        spyCamundaProxyApi.addResponseVariablesToJsonBuilder("booleanVar", true, jsonObjectBuilder);
        assertEquals(true, jsonObjectBuilder.build().getBoolean("booleanVar"));

        jsonObjectBuilder = createObjectBuilder();
        spyCamundaProxyApi.addResponseVariablesToJsonBuilder("intVar", 42, jsonObjectBuilder);
        assertEquals(42, jsonObjectBuilder.build().getInt("intVar"));

        jsonObjectBuilder = createObjectBuilder();
        spyCamundaProxyApi.addResponseVariablesToJsonBuilder("longVar", 42L, jsonObjectBuilder);
        assertEquals(42L, jsonObjectBuilder.build().getJsonNumber("longVar").longValue());

        jsonObjectBuilder = createObjectBuilder();
        spyCamundaProxyApi.addResponseVariablesToJsonBuilder("doubleVar", 42.42, jsonObjectBuilder);
        assertEquals(42.42, jsonObjectBuilder.build().getJsonNumber("doubleVar").doubleValue());

        jsonObjectBuilder = createObjectBuilder();
        BigDecimal bigDecimal = new BigDecimal("42.42");
        spyCamundaProxyApi.addResponseVariablesToJsonBuilder("bigDecimalVar", bigDecimal, jsonObjectBuilder);
        assertEquals(bigDecimal, jsonObjectBuilder.build().getJsonNumber("bigDecimalVar").bigDecimalValue());

        jsonObjectBuilder = createObjectBuilder();
        BigInteger bigInteger = new BigInteger("42");
        spyCamundaProxyApi.addResponseVariablesToJsonBuilder("bigIntegerVar", bigInteger, jsonObjectBuilder);
        assertEquals(bigInteger, jsonObjectBuilder.build().getJsonNumber("bigIntegerVar").bigIntegerValue());

        jsonObjectBuilder = createObjectBuilder();
        spyCamundaProxyApi.addResponseVariablesToJsonBuilder("stringVar", "stringValue", jsonObjectBuilder);
        assertEquals("stringValue", jsonObjectBuilder.build().getString("stringVar"));

        jsonObjectBuilder = createObjectBuilder();
        String defendantsJson = "[{\"id\":\"123\",\"firstName\":\"John\",\"lastName\":\"Doe\"},{\"id\":\"124\",\"firstName\":\"Jane\",\"lastName\":\"Doe\"}]";
        spyCamundaProxyApi.addResponseVariablesToJsonBuilder("DEFENDANTS", defendantsJson, jsonObjectBuilder);

        JsonArrayBuilder expectedDefendantsArray = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("id", "123")
                        .add("firstName", "John")
                        .add("lastName", "Doe"))
                .add(createObjectBuilder()
                        .add("id", "124")
                        .add("firstName", "Jane")
                        .add("lastName", "Doe"));

        assertEquals(expectedDefendantsArray.build(), jsonObjectBuilder.build().getJsonArray("DEFENDANTS"));
    }

    @Test
    void testGetJsonArrayBuilder() {
        String jsonValue = "[{\"id\":\"1\", \"firstName\":\"John\", \"lastName\":\"Doe\"}, {\"defendantId\":\"2\", \"firstName\":\"Jane\", \"lastName\":\"Doe\"}]";

        JsonArrayBuilder result = camundaProxyApi.getJsonArrayBuilder(jsonValue);

        JsonArrayBuilder expectedJsonArrayBuilder = createArrayBuilder();
        JsonObject jsonObject1 = createObjectBuilder()
                .add("id", "1")
                .add("firstName", "John")
                .add("lastName", "Doe")
                .build();
        JsonObject jsonObject2 = createObjectBuilder()
                .add("id", "2")
                .add("firstName", "Jane")
                .add("lastName", "Doe")
                .build();
        expectedJsonArrayBuilder.add(jsonObject1);
        expectedJsonArrayBuilder.add(jsonObject2);

        assertEquals(expectedJsonArrayBuilder.build(), result.build());
    }

    @Test
    void testAddVariablesToJsonBuilder() {
        CamundaProxyApi spyCamundaProxyApi = spy(new CamundaProxyApi());

        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        when(variableInstance.getName()).thenReturn("booleanVar");
        doReturn(true).when(spyCamundaProxyApi).getValueOfVarInstanceVariable(variableInstance);
        spyCamundaProxyApi.addVariablesToJsonBuilder(List.of(variableInstance), jsonObjectBuilder);

        assertEquals(true, jsonObjectBuilder.build().getBoolean("booleanVar"));

        jsonObjectBuilder = createObjectBuilder();
        when(variableInstance.getName()).thenReturn("intVar");
        doReturn(17).when(spyCamundaProxyApi).getValueOfVarInstanceVariable(variableInstance);
        spyCamundaProxyApi.addVariablesToJsonBuilder(List.of(variableInstance), jsonObjectBuilder);

        assertEquals(17, jsonObjectBuilder.build().getInt("intVar"));

        jsonObjectBuilder = createObjectBuilder();
        when(variableInstance.getName()).thenReturn("longVar");
        doReturn(21L).when(spyCamundaProxyApi).getValueOfVarInstanceVariable(variableInstance);
        spyCamundaProxyApi.addVariablesToJsonBuilder(List.of(variableInstance), jsonObjectBuilder);

        assertEquals(21L, jsonObjectBuilder.build().getJsonNumber("longVar").longValue());

        jsonObjectBuilder = createObjectBuilder();
        when(variableInstance.getName()).thenReturn("doubleVar");
        doReturn(42.42).when(spyCamundaProxyApi).getValueOfVarInstanceVariable(variableInstance);
        spyCamundaProxyApi.addVariablesToJsonBuilder(List.of(variableInstance), jsonObjectBuilder);

        assertEquals(42.42, jsonObjectBuilder.build().getJsonNumber("doubleVar").doubleValue());

        jsonObjectBuilder = createObjectBuilder();
        BigDecimal bigDecimal = new BigDecimal("54.54");
        when(variableInstance.getName()).thenReturn("bigDecimalVar");
        doReturn(bigDecimal).when(spyCamundaProxyApi).getValueOfVarInstanceVariable(variableInstance);
        spyCamundaProxyApi.addVariablesToJsonBuilder(List.of(variableInstance), jsonObjectBuilder);

        assertEquals(bigDecimal, jsonObjectBuilder.build().getJsonNumber("bigDecimalVar").bigDecimalValue());

        jsonObjectBuilder = createObjectBuilder();
        BigInteger bigInteger = new BigInteger("65");
        when(variableInstance.getName()).thenReturn("bigIntegerVar");
        doReturn(bigInteger).when(spyCamundaProxyApi).getValueOfVarInstanceVariable(variableInstance);
        spyCamundaProxyApi.addVariablesToJsonBuilder(List.of(variableInstance), jsonObjectBuilder);

        assertEquals(bigInteger, jsonObjectBuilder.build().getJsonNumber("bigIntegerVar").bigIntegerValue());

        jsonObjectBuilder = createObjectBuilder();
        String stringValue = "stringValue";
        when(variableInstance.getName()).thenReturn("stringVar");
        doReturn(stringValue).when(spyCamundaProxyApi).getValueOfVarInstanceVariable(variableInstance);
        spyCamundaProxyApi.addVariablesToJsonBuilder(List.of(variableInstance), jsonObjectBuilder);

        assertEquals(stringValue, jsonObjectBuilder.build().getString("stringVar"));

        jsonObjectBuilder = createObjectBuilder();
        String defendantsValue = "[{\"id\":\"123\", \"firstName\":\"John\", \"lastName\":\"Doe\"}, {\"defendantId\":\"124\", \"firstName\":\"Jane\", \"lastName\":\"Doe\"}]";
        JsonArray defendantJsonArray = createArrayBuilder()
                .add(createObjectBuilder().add("id", "123").add("firstName", "John").add("lastName", "Doe"))
                .add(createObjectBuilder().add("id", "124").add("firstName", "Jane").add("lastName", "Doe"))
                .build();
        when(variableInstance.getName()).thenReturn("defendants");
        doReturn(defendantsValue).when(spyCamundaProxyApi).getValueOfVarInstanceVariable(variableInstance);
        JsonArrayBuilder jsonArrayBuilder = createArrayBuilder()
                .add(createObjectBuilder().add("id", "123").add("firstName", "John").add("lastName", "Doe"))
                .add(createObjectBuilder().add("id", "124").add("firstName", "Jane").add("lastName", "Doe"));
        doReturn(jsonArrayBuilder).when(spyCamundaProxyApi).getJsonArrayBuilder(defendantsValue);
        spyCamundaProxyApi.addVariablesToJsonBuilder(List.of(variableInstance), jsonObjectBuilder);

        assertEquals(defendantJsonArray, jsonObjectBuilder.build().getJsonArray("defendants"));
    }

    @Test
    void testGetValueOfVarInstanceVariable_NullInstance() {
        VariableInstance instance = null;
        Object result = camundaProxyApi.getValueOfVarInstanceVariable(instance);
        assertEquals("", result);
    }

    @Test
    void testGetValueOfVarInstanceVariable_NullValue() {
        VariableInstance instance = mock(VariableInstance.class);
        when(instance.getValue()).thenReturn(null);

        Object result = camundaProxyApi.getValueOfVarInstanceVariable(instance);
        assertEquals("", result);
    }

    @Test
    void testGetValueOfVarInstanceVariable_DateType() {
        Date date = Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant());
        VariableInstance instance = mock(VariableInstance.class);
        when(instance.getValue()).thenReturn(date);
        when(instance.getTypeName()).thenReturn(PrimitiveValueType.DATE.getName());

        Object result = camundaProxyApi.getValueOfVarInstanceVariable(instance);
        assertEquals(dateFormat.format(date), result);
    }

    @Test
    void testGetValueOfVarInstanceVariable_OtherType() {
        String value = "someValue";
        VariableInstance instance = mock(VariableInstance.class);
        when(instance.getValue()).thenReturn(value);
        when(instance.getTypeName()).thenReturn(PrimitiveValueType.STRING.getName());

        Object result = camundaProxyApi.getValueOfVarInstanceVariable(instance);
        assertEquals(value, result);
    }

    private static final String COMPLETION_REASON = "completionReason";

    private void createProcessInstanceWithTaskAndVariablesWithTaskStatus(final boolean isHearingDateExist, boolean hasTaskComment) {
        Date dueDate = convertToDate("2022-05-23T10:00:00.000Z");
        Date hearingDate = dueDate;
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, organisationIdStr);
        runtimeService.setVariable(processInstanceId, CASE_URN, CASE_URN_VAL);
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        final Task taskOne = activeTasks.get(0);
        taskId = taskOne.getId();
        final Task taskTwo = activeTasks.get(1);

        if (hasTaskComment) {
            taskService.createComment(taskId, processInstanceId, TASK_ONE_COMMENT_VAL);
        }

        taskOne.setDueDate(dueDate);

        taskService.saveTask(taskOne);
        taskTwo.setDueDate(dueDate);
        taskService.saveTask(taskTwo);
        taskService.setAssignee(taskOne.getId(), "testAssignee1");
        taskService.setAssignee(taskTwo.getId(), "testAssignee1");
        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is(TASK_ONE_NAME_VAL));
        assertThat(taskTwo.getName(), is(TASK_SECOND_NAME_VAL));

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, CASE_URN_VAL);
        if (isHearingDateExist) {
            taskOneVariables.put(HEARING_DATE, hearingDate);
        }
        taskOneVariables.put(HEARING_TYPE, HEARING_TYPE_VAL);
        taskOneVariables.put(REGION, CANDIDATE_GROUP_HMCTS_VAL);
        taskOneVariables.put(CASE_ID, CASE_ID_VAL);
        taskOneVariables.put(TASK_TYPE, TASK_ONE_TYPE_VAL);
        taskOneVariables.put(TASK_TYPE_ID, TASK_ONE_TYPE_ID_VAL);
        taskOneVariables.put(ACTIVITY_TYPES, TASK_ONE_CUSTOM_TASK_TYPE_VAL);
        taskOneVariables.put(ORGANISATION_ID, organisationIdStr);
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), CANDIDATE_GROUP_HMCTS_VAL);
        taskService.addCandidateGroup(taskOne.getId(), "random_hmcts_group");

        taskService.setAssignee(taskTwo.getId(), "testAssignee1");
        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, CASE_URN_VAL);
        if (isHearingDateExist) {
            taskTwoVariables.put(HEARING_DATE, hearingDate);
        }
        taskTwoVariables.put(HEARING_TYPE, HEARING_TYPE_VAL);
        taskTwoVariables.put(REGION, REGION_CPS_VAL);
        taskTwoVariables.put(ACTIVITY_TYPES, TASK_SECOND_ACTIVITY_TYPE_VAL);
        taskOneVariables.put(CASE_ID, organisationIdStr);
        taskOneVariables.put(TASK_TYPE, "taskType2");
        taskOneVariables.put(TASK_TYPE_ID, randomUUID());
        taskOneVariables.put(WORK_QUEUE, organisationIdStr);
        taskTwoVariables.put(ORGANISATION_ID, organisationIdStr);
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");
        taskService.addCandidateGroup(taskTwo.getId(), "random_cps_group");

        deleteTaskApi.completeTask(getCompleteJsonEnvelope(taskId, "completed"));

        final List<Task> activeTasksAfterDeletion = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasksAfterDeletion, hasSize(2));

    }

    private JsonEnvelope getCompleteJsonEnvelope(final String firstTaskId, final String completionReason) {
        final JsonObject payload = createObjectBuilder().add("taskId", firstTaskId).add(COMPLETION_REASON, completionReason).build();
        return envelopeFrom(metadataBuilder().withName("complete-task").withId(randomUUID()).withUserId(USER_ID), payload);
    }

    private WorkflowTaskType createWorkflowTaskType(final String taskName) {
        return WorkflowTaskType.builder()
                .taskName(taskName)
                .displayName(taskName)
                .build();
    }
}
