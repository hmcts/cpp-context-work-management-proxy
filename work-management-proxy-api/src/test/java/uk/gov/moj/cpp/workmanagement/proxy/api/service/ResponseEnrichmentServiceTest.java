package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.mapper.TaskMapper;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.TaskResponse;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"squid:S2187"})
@ExtendWith({MockitoExtension.class, ProcessEngineExtension.class})
@Deployment(resources = {"sample-test-process.bpmn"})
public class ResponseEnrichmentServiceTest {
    private static final String PROCESS_KEY_VALUE = "sample_test_custom_task_process";


    public static final String DUE_DATE = "due";
    public static final String DUE_DAYS_WITH_IN = "dueDaysWithIn";
    public static final String CASE_URN = "caseURN";
    public static final String ASSIGNEE = "assignee";
    public static final String ACTIVITY_TYPES = "activityTypes";
    public static final String CANDIDATE_GROUP = "candidateGroups";
    public static final String ORGANISATION_ID = "organisationId";
    public static final String IS_ASSIGNED = "isAssigned";
    private static final String RESPONSE_DEEP_LINK = "deepLink";
    private static final String RESPONSE_WORK_QUEUE = "workQueueName";
    @RegisterExtension
    ProcessEngineExtension extension = ProcessEngineExtension.builder()
            .configurationResource("camunda.cfg.xml")
            .build();

    private ProcessInstance processInstance;

    private TaskFilterService taskFilterService = new TaskFilterService();

    private final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);


    private ResponseEnrichmentService responseEnrichmentService = new ResponseEnrichmentService();

    @Mock
    private Requester requester;

    private CamundaJavaApiService camundaJavaApiService = new CamundaJavaApiService();

    protected TaskService taskService;
    protected RuntimeService runtimeService;

    @BeforeEach
    public void setUp() throws Exception {
        taskService = extension.getProcessEngine().getTaskService();
        runtimeService = extension.getProcessEngine().getRuntimeService();
        setField(taskFilterService, "taskService", taskService);
        setField(camundaJavaApiService, "taskService", taskService);
        setField(camundaJavaApiService, "runtimeService", runtimeService);
        setField(camundaJavaApiService, "taskFilterService", taskFilterService);
    }

    @Test
    public void shouldEnrichTaskVariables() {

        createProcessInstanceWithTaskAndVariables();
        final List<TaskWithVariables> tasksWithVariables = createTaskWithVariablesList();

        final List<TaskResponse> taskResponses = responseEnrichmentService.buildAndEnrichTaskResponseList(tasksWithVariables);
        assertThat(taskResponses, notNullValue());
        assertEquals(taskResponses.size(), 1);
        final TaskResponse taskResponse = taskResponses.get(0);
        assertEquals(taskResponse.getName(), "First Task");
        assertEquals(taskResponse.getTaskDefinitionKey(), "firstTask");
    }

    public List<TaskWithVariables> createTaskWithVariablesList() {
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();

        final Task taskOne = activeTasks.get(0);
        List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableInstanceList = new ArrayList<>();
        TaskWithVariables taskWithVariablesOne = new TaskWithVariables(TaskMapper.TASK_MAPPER.taskToTaskDto(taskOne));
        taskWithVariablesOne.setVariableList(variableInstanceList);
        List<TaskWithVariables> taskWithVariablesList = new ArrayList<>();
        taskWithVariablesList.add(taskWithVariablesOne);

        return taskWithVariablesList;
    }

    public void createProcessInstanceWithTaskAndVariables() {

        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();

        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);
        taskOne.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskOne);
        taskTwo.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        assertThat(taskTwo.getName(), is("Second Task"));

        taskService.setAssignee(taskOne.getId(), "testAssignee1");

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(WORK_QUEUE, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");

        taskService.setAssignee(taskTwo.getId(), "testAssignee2");
        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(WORK_QUEUE, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskTwoVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");

    }

}
