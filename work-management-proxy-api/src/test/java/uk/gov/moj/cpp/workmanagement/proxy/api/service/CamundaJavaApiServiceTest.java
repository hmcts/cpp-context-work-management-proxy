package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ACTIVITY_TYPES;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ASSIGNEE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ASSIGN_TO_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.BUSINESS_UNIT_CODES;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANCELLATION_REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANDIDATE_GROUPS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_URN;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COURT_CODES;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CUSTODY_TIME_LIMIT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CUSTOM_TASK_TYPE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DEFENDANTS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DUE_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.FOLLOW_UP;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.HEARING_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.HEARING_TYPE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_CTL_ONLY;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_URGENT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LAST_UPDATED_BY_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LAST_UPDATED_BY_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LIMIT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.NOTE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.NOTE_LAST_UPDATED_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.NOTE_LAST_UPDATED_USER;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.OFFSET;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ORGANISATION_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.PREVIOUS_DUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.REGION;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_TYPE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_TYPE_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_VARIABLES_JSON_STRING;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.USER_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils.convertToDate;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.ActivitySummary;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.FilteredTasks;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowTaskType;
import uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants;

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
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.exception.NullValueException;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, ProcessEngineExtension.class})
@Deployment(resources = {"sample-test-process.bpmn", "sample-test-process-2.bpmn", "custom-task-process.bpmn"})
public class CamundaJavaApiServiceTest {

    /*This is sample process deployed to run the test cases. process file is present in the resources folder
     * Should file be changed then below process specific variables should be replaced with new uploaded process key */
    private static final String PROCESS_KEY_VALUE = "sample_test_custom_task_process";

    private final DateFormat dueDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final String DUE_DATE_STRING = "2024-07-28T22:59:00.000Z";
    private static final String DUE_DATE_STRING_WITHOUT_ZONE = "2024-07-28T22:59:00";
    private static String taskId = "11";

    @RegisterExtension
    ProcessEngineExtension extension = ProcessEngineExtension.builder()
            .configurationResource("camunda.cfg.xml")
            .build();

    private ProcessInstance processInstance;

    @Spy
    private ObjectMapperProducer objectMapperProducer;

    @Spy
    @InjectMocks
    private CamundaJavaApiService camundaJavaApiService = new CamundaJavaApiService();

    private TaskFilterService taskFilterService = new TaskFilterService();

    private final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

    @Mock
    private final UserService userService = new UserService();

    @Mock
    private final ReferenceDataService referenceDataService = new ReferenceDataService();

    private String userDetails = "Jane Doe";
    protected TaskService taskService;
    protected RuntimeService runtimeService;
    protected ProcessEngine processEngine;

    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        taskService = extension.getProcessEngine().getTaskService();
        runtimeService = extension.getProcessEngine().getRuntimeService();
        processEngine = extension.getProcessEngine();
        historyService = extension.getProcessEngine().getHistoryService();
        setField(taskFilterService, "taskService", taskService);
        setField(camundaJavaApiService, "taskService", taskService);
        setField(camundaJavaApiService, "runtimeService", runtimeService);
        setField(camundaJavaApiService, "processEngine", processEngine);
        setField(camundaJavaApiService, "taskFilterService", taskFilterService);
        setField(camundaJavaApiService, "userService", userService);
        setField(camundaJavaApiService, "referenceDataService", referenceDataService);
        setField(camundaJavaApiService, "historyService", historyService);
        setField(taskFilterService, "historyService", historyService);
        setField(taskFilterService, "runtimeService", runtimeService);
        setField(camundaJavaApiService, "objectMapperProducer", objectMapperProducer);
    }


    private void createProcessInstanceWithUnassignedTaskAndVariables(final long createTimeGap, final String taskName) {
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        runtimeService.setVariable(processInstanceId, CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        runtimeService.setVariable(processInstanceId, REGION, "CPS");
        runtimeService.setVariable(processInstanceId, TASK_NAME, taskName);
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        final Date createTime = Date.from(now().minusDays(createTimeGap).atStartOfDay(ZoneId.systemDefault()).toInstant());

        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);
        taskOne.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOne.setAssignee("ASSIGNEE-1");

        taskOne.getCreateTime().setTime(createTime.getTime());
        taskService.saveTask(taskOne);
        taskTwo.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).plusMinutes(1).toInstant()));
        taskTwo.setAssignee("ASSIGNEE-2");
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        assertThat(taskTwo.getName(), is("Second Task"));

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(HEARING_DATE, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(HEARING_TYPE, "hearing_type_1");
        taskOneVariables.put(WORK_QUEUE, "ABC");
        taskOneVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(NOTE, "Note");
        taskOneVariables.put(NOTE_LAST_UPDATED_DATE, ZonedDateTime.now().minusDays(4).toString());
        taskOneVariables.put(NOTE_LAST_UPDATED_USER, "Erica Wilson");
        taskOneVariables.put(REGION, "CPS");
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");

        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskTwoVariables.put(HEARING_DATE, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(HEARING_TYPE, "hearing_type_1");
        taskTwoVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(NOTE, "Note");
        taskOneVariables.put(NOTE_LAST_UPDATED_DATE, ZonedDateTime.now().toString());
        taskOneVariables.put(NOTE_LAST_UPDATED_USER, "Emma Cleaner");
        taskTwoVariables.put(REGION, "CPS");
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");
    }

    private void createProcessInstanceWithTaskAndVariables() {
        final Date dueDate = convertToDate("2022-05-23T10:00:00.000Z");

        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        runtimeService.setVariable(processInstanceId, CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        runtimeService.setVariable(processInstanceId, TASK_NAME, "secondTask");

        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();

        final Task taskOne = activeTasks.get(0);
        taskId = taskOne.getId();
        taskOne.setDueDate(dueDate);
        taskOne.setAssignee("ASSIGNEE-1");
        taskService.saveTask(taskOne);

        final Task taskTwo = activeTasks.get(1);
        taskTwo.setDueDate(dueDate);
        taskTwo.setAssignee("ASSIGNEE-2");
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        assertThat(taskTwo.getName(), is("Second Task"));

        taskService.setAssignee(taskOne.getId(), "testAssignee1");

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(HEARING_DATE, dueDate);
        taskOneVariables.put(DUE_DATE, dueDate);
        taskOneVariables.put(HEARING_TYPE, "hearing_type_1");
        taskOneVariables.put(REGION, "HMCTS");
        taskOneVariables.put(TASK_TYPE, "taskType");
        taskOneVariables.put(TASK_TYPE_ID, randomUUID());
        taskOneVariables.put(ACTIVITY_TYPES, "firstTask");
        taskOneVariables.put(CASE_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(WORK_QUEUE, "e7be77a9-9ac5-431d-a23b-2d750e441b76");
        taskOneVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b77");
        taskOneVariables.put(CUSTOM_TASK_TYPE, "activity");
        taskOneVariables.put(COURT_CODES, "12345");
        taskOneVariables.put(BUSINESS_UNIT_CODES, "56789");
        taskOneVariables.put(FOLLOW_UP, dueDate);
        taskOneVariables.put(DEFENDANTS, "[{\"id\":\"1\",\"firstName\":\"Smith\",\"lastName\":\"Philip\"}]");
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");

        taskService.createComment(taskId, processInstanceId, "Comment1");
        taskService.createComment(taskId, processInstanceId, "Comment2");

        taskService.setAssignee(taskTwo.getId(), "testAssignee2");
        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskTwoVariables.put(HEARING_DATE, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(HEARING_TYPE, "hearing_type_1");
        taskTwoVariables.put(REGION, "CPS");
        taskTwoVariables.put(ACTIVITY_TYPES, "secondTask");
        taskTwoVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");
    }


    @Test
    void testGetCustomQueryAssigneeTasksWithVariablesIllegalArgumentException() throws ParseException {

        createProcessInstanceWithTaskAndVariables();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .build());
        try {
            camundaJavaApiService.queryTasksAssigneeWithVariables(jsonEnvelope);
        } catch (Exception e) {
            assertThat(e.toString(), e instanceof IllegalArgumentException);
        }
    }

    @Test
    void testGetCustomQueryAssigneeTasksWithVariablesNoFilters() throws ParseException {

        createProcessInstanceWithTaskAndVariables();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(ASSIGNEE, "testAssignee1")
                .build());
        final FilteredTasks filteredTasks = camundaJavaApiService.queryTasksAssigneeWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        for (TaskWithVariables tasks : tasksWithVariables) {
            final uk.gov.moj.cpp.workmanagement.proxy.api.model.Task task = tasks.getTask();
            final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableList = tasks.getVariableList();
            assertThat(task.getAssignee(), is("testAssignee1"));
            assertThat(task.getName(), is("First Task"));
            assertThat(task.getDueDate(), notNullValue());

            final Map<String, Object> varMap = new HashMap<>();
            variableList.forEach(v -> varMap.put(v.getName(), v.getValue()));
            assertThat(varMap.get(CUSTODY_TIME_LIMIT), notNullValue());
            assertThat(varMap.get(HEARING_TYPE), notNullValue());
            assertThat(varMap.get(HEARING_DATE), notNullValue());
            assertThat(varMap.get(IS_URGENT), notNullValue());
        }
    }

    @Test
    void testGetCustomQueryAssigneeTasksWithVariablesAllFilters() {

        createProcessInstanceWithTaskAndVariables();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(ASSIGNEE, "testAssignee2")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryTasksAssigneeWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        for (TaskWithVariables tasks : tasksWithVariables) {
            final uk.gov.moj.cpp.workmanagement.proxy.api.model.Task task = tasks.getTask();
            final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableList = tasks.getVariableList();
            assertThat(task.getAssignee(), is("testAssignee2"));
            assertThat(task.getName(), is("Second Task"));
            assertThat(task.getDueDate(), notNullValue());

            final Map<String, Object> varMap = new HashMap<>();
            variableList.forEach(v -> varMap.put(v.getName(), v.getValue()));
            assertThat(varMap.get(CUSTODY_TIME_LIMIT), notNullValue());
            assertThat(varMap.get(HEARING_TYPE), notNullValue());
            assertThat(varMap.get(HEARING_DATE), notNullValue());
            assertThat(varMap.get(IS_URGENT), notNullValue());
        }
    }

    @Test
    void testGetCustomQueryAssigneeTasksWithVariablesAllFiltersAndPagination() {

        for (int i = 0; i < 50; i++) {
            createProcessInstanceWithTaskAndVariables();
        }

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(ASSIGNEE, "testAssignee2")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(OFFSET, 0)
                .add(LIMIT, 50)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryTasksAssigneeWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables.size(), is(50));

        for (TaskWithVariables tasks : tasksWithVariables) {
            final uk.gov.moj.cpp.workmanagement.proxy.api.model.Task task = tasks.getTask();
            final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableList = tasks.getVariableList();
            assertThat(task.getAssignee(), is("testAssignee2"));
            assertThat(task.getName(), is("Second Task"));
            assertThat(task.getDueDate(), notNullValue());

            final Map<String, Object> varMap = new HashMap<>();
            variableList.forEach(v -> varMap.put(v.getName(), v.getValue()));
            assertThat(varMap.get(CUSTODY_TIME_LIMIT), notNullValue());
            assertThat(varMap.get(HEARING_TYPE), notNullValue());
            assertThat(varMap.get(HEARING_DATE), notNullValue());
            assertThat(varMap.get(IS_URGENT), notNullValue());
        }
    }

    @Test
    void testGetCustomQueryAvailableTasksWithVariablesNoFilters() {

        createProcessInstanceWithUnassignedTaskAndVariables(0, "firstTask");

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "firstTask")
                .add(ASSIGNEE, "ASSIGNEE-1")
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        for (TaskWithVariables tasks : tasksWithVariables) {
            final uk.gov.moj.cpp.workmanagement.proxy.api.model.Task task = tasks.getTask();
            final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableList = tasks.getVariableList();
            assertThat(task.getAssignee(), is("ASSIGNEE-1"));
            assertThat(task.getName(), is("First Task"));
            assertThat(task.getDueDate(), notNullValue());

            final Map<String, Object> varMap = new HashMap<>();
            variableList.forEach(v -> varMap.put(v.getName(), v.getValue()));
            assertThat(varMap.get(CUSTODY_TIME_LIMIT), notNullValue());
            assertThat(varMap.get(HEARING_TYPE), notNullValue());
            assertThat(varMap.get(HEARING_DATE), notNullValue());
            assertThat(varMap.get(IS_URGENT), notNullValue());
            assertThat(varMap.get(REGION), is("CPS"));
        }

    }

    @Test
    void testGetCustomQueryAvailableTasksWithVariablesAllFilters() {

        createProcessInstanceWithUnassignedTaskAndVariables(0, "firstTask");

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "firstTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-1")
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        for (TaskWithVariables tasks : tasksWithVariables) {
            final uk.gov.moj.cpp.workmanagement.proxy.api.model.Task task = tasks.getTask();
            final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableList = tasks.getVariableList();
            assertThat(task.getAssignee(), notNullValue());
            assertThat(task.getName(), is("First Task"));
            assertThat(task.getDueDate(), notNullValue());

            final Map<String, Object> varMap = new HashMap<>();
            variableList.forEach(v -> varMap.put(v.getName(), v.getValue()));
            assertThat(varMap.get(CUSTODY_TIME_LIMIT), notNullValue());
            assertThat(varMap.get(HEARING_TYPE), notNullValue());
            assertThat(varMap.get(HEARING_DATE), notNullValue());
            assertThat(varMap.get(IS_URGENT), notNullValue());
            assertThat(varMap.get(REGION), is("CPS"));
        }

    }

    @Test
    void testGetCustomQueryAvailableTasksWithVariablesAllFiltersAndPagination() {

        for (int i = 0; i < 50; i++) {
            createProcessInstanceWithUnassignedTaskAndVariables(0, "firstTask");
            createProcessInstanceWithUnassignedTaskAndVariables(0, "secondTask");
        }

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-2")
                .add(OFFSET, 0)
                .add(LIMIT, 50)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables.size(), is(50));

        for (TaskWithVariables tasks : tasksWithVariables) {
            final uk.gov.moj.cpp.workmanagement.proxy.api.model.Task task = tasks.getTask();
            final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableList = tasks.getVariableList();
            assertThat(task.getAssignee(), notNullValue());
            assertThat(task.getName(), is("Second Task"));
            assertThat(task.getDueDate(), notNullValue());

            final Map<String, Object> varMap = new HashMap<>();
            variableList.forEach(v -> varMap.put(v.getName(), v.getValue()));
            assertThat(varMap.get(CUSTODY_TIME_LIMIT), notNullValue());
            assertThat(varMap.get(HEARING_TYPE), notNullValue());
            assertThat(varMap.get(HEARING_DATE), notNullValue());
            assertThat(varMap.get(IS_URGENT), notNullValue());
            assertThat(varMap.get(REGION), is("CPS"));
        }
    }

    @Test
    void shouldGetActivitySummary() {

        createProcessInstanceWithUnassignedTaskAndVariables(0, "firstTask");

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .build());

        final ActivitySummary activitySummary = camundaJavaApiService.queryActivitySummary(jsonEnvelope);
        assertNotNull(activitySummary);
        assertEquals(2, activitySummary.getTotalActivities());
        assertThat(activitySummary.getActivities().get(0).getTaskName(), is(equalTo("firstTask")));

        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getOneToSevenDaysCount(), is(equalTo(1)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getEightToFourteenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getTotal(), is(equalTo(1)));

        assertThat(activitySummary.getActivities().get(0).getActivityStat().getUnassigned().getOneToSevenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getUnassigned().getEightToFourteenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getUnassigned().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getUnassigned().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getUnassigned().getTotal(), is(equalTo(0)));

        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getOneToSevenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getEightToFourteenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getTotal(), is(equalTo(0)));

        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getOneToSevenDaysCount(), is(equalTo(1)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getEightToFourteenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getTotal(), is(equalTo(1)));

        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getOneToSevenDaysCount(), is(equalTo(1)));
        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getEightToFourteenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getTotal(), is(equalTo(1)));
    }

    @Test
    void shouldGetActivitySummaryWhenTaskCreateTimeMoreThanSevenDaysLessThanFourteenDays() {

        createProcessInstanceWithUnassignedTaskAndVariables(10, "firstTask");

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .build());

        final ActivitySummary activitySummary = camundaJavaApiService.queryActivitySummary(jsonEnvelope);
        assertNotNull(activitySummary);
        assertEquals(2, activitySummary.getTotalActivities());
        assertThat(activitySummary.getActivities().get(0).getTaskName(), is(equalTo("firstTask")));

        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getOneToSevenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getEightToFourteenDaysCount(), is(equalTo(1)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getAssigned().getTotal(), is(equalTo(1)));

        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getOneToSevenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getEightToFourteenDaysCount(), is(equalTo(1)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getOverall().getTotal(), is(equalTo(1)));

    }


    @Test
    void shouldGetActivitySummaryWhenCreateDateAfterDueDate() {

        createProcessInstanceWithUnassignedTaskAndVariables(-3, "firstTask");

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .build());

        final ActivitySummary activitySummary = camundaJavaApiService.queryActivitySummary(jsonEnvelope);
        assertNotNull(activitySummary);
        assertEquals(2, activitySummary.getTotalActivities());
        assertThat(activitySummary.getActivities().get(0).getTaskName(), is(equalTo("firstTask")));

        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getOneToSevenDaysCount(), is(equalTo(1)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getEightToFourteenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(0).getActivityStat().getEscalated().getTotal(), is(equalTo(1)));

        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getOneToSevenDaysCount(), is(equalTo(1)));
        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getEightToFourteenDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getFifteenToTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getOverTwentyOneDaysCount(), is(equalTo(0)));
        assertThat(activitySummary.getActivities().get(1).getActivityStat().getOverall().getTotal(), is(equalTo(1)));
    }

    @Test
    void shouldUpdateDueDate() {

        when(userService.getUserDetails(any())).thenReturn(userDetails);
        createProcessInstanceWithUnassignedTaskAndVariables(0, "firstTask");

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "firstTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-1")
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

        final TaskWithVariables tasksWithVariables2 = camundaJavaApiService.updateTask(jsonEnvelope);

        assertThat(dueDateFormat.format(tasksWithVariables2.getTask().getDueDate()), is(DUE_DATE_STRING_WITHOUT_ZONE));

    }

    @Test
    void shouldNotUpdateLastUpdatedDateWhenDueDateUpdated() {

        when(userService.getUserDetails(any())).thenReturn(userDetails);

        createProcessInstanceWithUnassignedTaskAndVariables(0, "secondTask");

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-2")
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
                .add(ContextConstants.DUE_DATE, DUE_DATE_STRING)
                .build());

        final TaskWithVariables tasksWithVariables2 = camundaJavaApiService.updateTask(jsonEnvelope);
        assertThat(dueDateFormat.format(tasksWithVariables2.getTask().getDueDate()), is(DUE_DATE_STRING_WITHOUT_ZONE));
        Object lastUpdatedDate = taskService.getVariable(tasksWithVariables2.getTask().getId(), NOTE_LAST_UPDATED_DATE);
        Object lastUpdatedUser = taskService.getVariable(tasksWithVariables2.getTask().getId(), NOTE_LAST_UPDATED_USER);
        assertThat(lastUpdatedDate, is(nullValue()));
        assertThat(lastUpdatedUser, is(nullValue()));

    }

    @Test
    void shouldUpdateNoteWithUpdatedDate() {

        when(userService.getUserDetails(any())).thenReturn(userDetails);

        createProcessInstanceWithUnassignedTaskAndVariables(0, "secondTask");

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId("testAssignee2")
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-2")
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        TaskWithVariables originalTask = tasksWithVariables.get(0);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("taskId", originalTask.getTask().getId())
                .add(NOTE, "new note").build());

        final TaskWithVariables tasksWithVariables2 = camundaJavaApiService.updateTask(jsonEnvelope);

        final Map<String, Object> variables = taskService.getVariables(originalTask.getTask().getId());
        assertThat(variables.get(NOTE), is("new note"));
        Object noteLastUpdatedDate = taskService.getVariable(tasksWithVariables2.getTask().getId(), NOTE_LAST_UPDATED_DATE);
        Object noteLastUpdatedUser = taskService.getVariable(tasksWithVariables2.getTask().getId(), NOTE_LAST_UPDATED_USER);
        assertThat(noteLastUpdatedDate, is(notNullValue()));
        final ZonedDateTime noteLastUpdatedZonedDateTime = ZonedDateTime.parse(noteLastUpdatedDate.toString());
        assertThat(noteLastUpdatedZonedDateTime.isAfter(ZonedDateTime.now().minusMinutes(1)), is(true));
        assertThat(noteLastUpdatedZonedDateTime.isBefore(ZonedDateTime.now()), is(true));
        assertThat(noteLastUpdatedUser, is("Jane Doe"));
    }

    @Test
    void shouldAssignTask() {

        when(userService.getUserDetails(any())).thenReturn(userDetails);

        createProcessInstanceWithUnassignedTaskAndVariables(0, "secondTask");
        final String assignerUser = "User1";
        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId(assignerUser)
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-2")
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        TaskWithVariables originalTask = tasksWithVariables.get(0);
        taskService.setAssignee(originalTask.getTask().getId(), assignerUser);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(USER_ID, "Test")
                .add(TASK_ID, originalTask.getTask().getId())
                .build());

        camundaJavaApiService.assignTask(jsonEnvelope);

        final Map<String, Object> variables = taskService.getVariables(originalTask.getTask().getId());
        assertThat(variables.get(LAST_UPDATED_BY_ID), is(assignerUser));
        assertThat(variables.get(ASSIGN_TO_NAME), is("Jane Doe"));
    }

    @Test
    void shouldUnClaimTask() {
        when(userService.getUserDetails(any())).thenReturn(userDetails);
        createProcessInstanceWithUnassignedTaskAndVariables(0, "secondTask");
        final String unClaimUser = "User1";
        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId(unClaimUser)
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-2")
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        TaskWithVariables originalTask = tasksWithVariables.get(0);
        taskService.setAssignee(originalTask.getTask().getId(), unClaimUser);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(USER_ID, "")
                .add(TASK_ID, originalTask.getTask().getId())
                .build());
        camundaJavaApiService.assignTask(jsonEnvelope);

        final Map<String, Object> variables = taskService.getVariables(originalTask.getTask().getId());
        assertThat(variables.get(LAST_UPDATED_BY_ID), is(unClaimUser));
        assertThat(variables.get(ASSIGN_TO_NAME), is(nullValue()));
    }

    @Test
    void shouldClaimTask() {
        when(userService.getUserDetails(any())).thenReturn(userDetails);

        createProcessInstanceWithUnassignedTaskAndVariables(0, "secondTask");
        final String claimUser = "User1";
        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId(claimUser)
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-2")
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        TaskWithVariables originalTask = tasksWithVariables.get(0);
        //   taskService.setAssignee(originalTask.getTask().getId(),"User");
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(USER_ID, claimUser)
                .add(TASK_ID, originalTask.getTask().getId())
                .build());
        camundaJavaApiService.assignTask(jsonEnvelope);

        final Map<String, Object> variables = taskService.getVariables(originalTask.getTask().getId());
        assertThat(variables.get(LAST_UPDATED_BY_ID), is(claimUser));
        assertThat(variables.get(ASSIGN_TO_NAME), is("Jane Doe"));
    }

    @Test
    void shouldClaimTaskWithoutUserDetails() {
        createProcessInstanceWithUnassignedTaskAndVariables(0, "secondTask");

        final String claimUser = "User1";
        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId(claimUser)
                .build());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(REGION, "CPS")
                .add(ACTIVITY_TYPES, "secondTask")
                .add(IS_URGENT, TRUE)
                .add(IS_CTL_ONLY, TRUE)
                .add(ASSIGNEE, "ASSIGNEE-2")
                .add(OFFSET, 0)
                .add(LIMIT, 20)
                .build());

        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(jsonEnvelope);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();

        assertThat(tasksWithVariables.size(), is(1));
        TaskWithVariables originalTask = tasksWithVariables.get(0);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(USER_ID, claimUser)
                .add(TASK_ID, originalTask.getTask().getId())
                .build());
        final TaskWithVariables updatedTask = camundaJavaApiService.assignTask(jsonEnvelope);

        final Map<String, Object> variables = taskService.getVariables(originalTask.getTask().getId());
        assertThat(variables.get(LAST_UPDATED_BY_ID), is(claimUser));
    }

    @Test
    void shouldReturnTaskDetailWhenTaskIdIsValid() throws ParseException {

        createProcessInstanceWithTaskAndVariables();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(TASK_ID, taskId)
                .build());

        final TaskWithVariables taskDetails = camundaJavaApiService.getTaskDetails(jsonEnvelope);

        assertThat(getDateForCompare(taskDetails.getTask().getDueDate()), is("2022-05-23"));
    }

    @Test
    void shouldReturnExceptionWhenTaskIdIsNotValid() throws ParseException {

        createProcessInstanceWithTaskAndVariables();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(TASK_ID, taskId)
                .build());
        try {
            camundaJavaApiService.getTaskDetails(jsonEnvelope);
        } catch (Exception e) {
            assertThat(e.toString(), e instanceof IllegalArgumentException);
        }
    }

    @Test
    void shouldReturnExceptionWhenTaskIdIsIsEmpty() throws ParseException {

        createProcessInstanceWithTaskAndVariables();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(TASK_ID, "")
                .build());
        try {
            final TaskWithVariables taskDetails = camundaJavaApiService.getTaskDetails(jsonEnvelope);
        } catch (Exception e) {
            assertThat(e.toString(), e instanceof BadRequestException);
        }
    }

    String getDateForCompare(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(date);
    }

    @Test
    void shouldReOpenTask() throws JsonProcessingException {
        final String userId = "User1";

        when(userService.getUserDetails(any())).thenReturn(userDetails);
        when(referenceDataService.getWorkFlowTaskTypeWithId(any()))
                .thenReturn(WorkflowTaskType.builder().taskName("sample_test_2").isCustomTask(true).build());
        createProcessInstanceWithTaskAndVariables();
        cancelTask(taskId);

        final MetadataBuilder metadataBuilder = metadataWithRandomUUIDAndName();
        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder
                .withUserId(userId)
                .build());
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add(TASK_ID, taskId)
                .add(ASSIGNEE, userId)
                .add(DUE_DATE, DUE_DATE_STRING)
                .add(REASON, "reopen task")
                .add(CANDIDATE_GROUPS, "hmcts_group")
                .build());

        final TaskWithVariables taskWithVariables = camundaJavaApiService.reopenTask(jsonEnvelope);

        assertThat(taskWithVariables.getTask().getName(), is("Fourth Task"));
        assertThat(taskWithVariables.getTask().getTaskDefinitionKey(), is("fourthTask"));

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder().add(TASK_ID, taskWithVariables.getTask().getId()).build());

        validateTaskVariablesForReopenActivity();

        final List<Comment> comments = taskService.getTaskComments(taskWithVariables.getTask().getId());

        List<String> fullMessageList = comments.stream().map(Comment::getFullMessage).collect(Collectors.toList());
        assertThat(fullMessageList, hasItems("Comment1", "Comment2", "Reopened - reopen task. Old activity id is " + taskId + "."));

        taskService.getIdentityLinksForTask(taskWithVariables.getTask().getId()).forEach(identityLink -> {
            if (StringUtils.isNotBlank(identityLink.getUserId())) {
                assertThat(identityLink.getUserId(), is(userId));
            }

            if (StringUtils.isNotBlank(identityLink.getGroupId())) {
                assertThat(identityLink.getGroupId(), is("hmcts_group"));
            }
        });
    }

    private void validateTaskVariablesForReopenActivity() {
        final TaskWithVariables taskDetails = camundaJavaApiService.getTaskDetails(jsonEnvelope);
        final Map<String, Object> variables = taskDetails.getVariableMap();

        assertThat(variables.get(TASK_VARIABLES_JSON_STRING), is(notNullValue()));
        assertThat(variables.get(TASK_TYPE), is("activity"));
        assertThat(variables.get(PREVIOUS_DUE), is(notNullValue()));
        assertThat(variables.get(LAST_UPDATED_BY_ID), is("User1"));
        assertThat(variables.get(USER_ID), is("User1"));
        assertThat(variables.get(ASSIGN_TO_NAME), is("Jane Doe"));
        assertThat(variables.get(LAST_UPDATED_BY_NAME), is("Jane Doe"));
    }

    private void cancelTask(final String taskId) {
        taskService.setVariableLocal(taskId, CANCELLATION_REASON, "no longer needed");
        taskService.complete(taskId);
    }

    @Test
    void shouldReturnExceptionWhenTaskIsNotFound() throws ParseException {

        createProcessInstanceWithTaskAndVariables();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder().add(TASK_ID, "44").build());
        try {
            final TaskWithVariables taskDetails = camundaJavaApiService.getTaskDetails(jsonEnvelope);
        } catch (Exception e) {
            assertThat(e.toString(), e instanceof BadRequestException);
        }
    }

    @Test
    void shouldReturnTaskDetailWhenTaskIdIsValidWithComments() throws ParseException {

        createProcessInstanceWithTaskAndVariablesWithAndWithoutComments(true);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder().add(TASK_ID, taskId).build());

        final TaskWithVariables taskDetails = camundaJavaApiService.getTaskDetails(jsonEnvelope);
        final List<Comment> comments = taskService.getTaskComments(taskDetails.getTask().getId());

        assertThat(comments.size(), is(2));
        assertThat(getDateForCompare(taskDetails.getTask().getDueDate()), is("2022-05-23"));
    }

    @Test
    void shouldReturnTaskDetailWhenTaskIdIsValidWithNoComments() throws ParseException {
        createProcessInstanceWithTaskAndVariablesWithAndWithoutComments(false);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder().add(TASK_ID, taskId).build());

        final TaskWithVariables taskDetails = camundaJavaApiService.getTaskDetails(jsonEnvelope);
        final List<Comment> comments = taskService.getTaskComments(taskDetails.getTask().getId());

        assertThat(comments.size(), is(0));
        assertThat(getDateForCompare(taskDetails.getTask().getDueDate()), is("2022-05-23"));
    }

    @Test
    void shouldCreateGenericTaskAndVariablesThrowException() throws ParseException {

        final JsonObject payload = createObjectBuilder()
                .add(ASSIGNEE, "testAssignee1")
                .add("candidateGroups", "Candidate Groups")
                .add("displayName", "Display name")
                .add("due", "22/04/2024")
                .add("taskTitle", "taskTitle")
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder()
                .withName("create-generic-task")
                .withId(randomUUID())
                .withUserId(USER_ID), payload);

        createProcessInstanceWithTaskAndVariablesWithAndWithoutComments(false);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder().add(TASK_ID, taskId).build());

        assertThrows(NullValueException.class,
                () -> camundaJavaApiService.createGenericTaskAndVariables(envelope));

    }

    private JsonObject createJsonMetaData() {
        return createObjectBuilder()
                .add("name", "create-generic-task")
                .add("id", UUID.randomUUID().toString())
                .add("userId", UUID.randomUUID().toString())
                .build();
    }

    private void createProcessInstanceWithTaskAndVariablesWithAndWithoutComments(Boolean flag) throws ParseException {
        final Date dueDate = convertToDate("2022-05-23T10:00:00.000Z");

        Boolean commentFlag = flag;
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();

        final Task taskOne = activeTasks.get(0);
        taskId = taskOne.getId();
        final Task taskTwo = activeTasks.get(1);
        taskOne.setDueDate(dueDate);
        taskOne.setAssignee("ASSIGNEE-1");
        taskService.saveTask(taskOne);
        taskTwo.setDueDate(dueDate);
        taskTwo.setAssignee("ASSIGNEE-2");
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        assertThat(taskTwo.getName(), is("Second Task"));

        taskService.setAssignee(taskOne.getId(), "testAssignee1");

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(HEARING_DATE, dueDate);
        taskOneVariables.put(HEARING_TYPE, "hearing_type_1");
        taskOneVariables.put(REGION, "HMCTS");
        taskOneVariables.put(TASK_TYPE, "taskType");
        taskOneVariables.put(ACTIVITY_TYPES, "firstTask");
        taskOneVariables.put(CASE_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(WORK_QUEUE, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");

        if (commentFlag) {
            taskService.createComment(taskId, processInstanceId, "comment1");
            taskService.createComment(taskId, processInstanceId, "comment2");
        }


        taskService.setAssignee(taskTwo.getId(), "testAssignee2");
        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskTwoVariables.put(HEARING_DATE, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(HEARING_TYPE, "hearing_type_1");
        taskTwoVariables.put(REGION, "CPS");
        taskTwoVariables.put(ACTIVITY_TYPES, "secondTask");
        taskTwoVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");
    }

    private WorkflowTaskType createWorkflowTaskType(final String taskName) {
        return WorkflowTaskType.builder()
                .taskName(taskName)
                .displayName(taskName)
                .build();
    }
}
