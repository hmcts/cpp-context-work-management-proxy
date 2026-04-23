package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.lang.Boolean.TRUE;
import static java.sql.Date.valueOf;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Date.from;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_TAG;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_DUE_SOON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_TYPE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils.convertToDate;

import uk.gov.moj.cpp.workmanagement.proxy.api.model.FilteredTasks;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowTaskType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, ProcessEngineExtension.class})
@Deployment(resources = {"sample-test-process.bpmn", "sample-test-process-3.bpmn"})
public class TaskFilterServiceTest {

    /*This is sample process deployed to run the test cases. process file is present in the resources folder
     * Should file be changed then below process specific variables should be replaced with new uploaded process key */
    private static final String PROCESS_KEY_VALUE = "sample_test_custom_task_process";
    private static final String PROCESS_KEY_3_VALUE = "sample_test_3_custom_task_process";
    private static final String DUE_DATE = "due";
    private static final String DUE_DAYS_WITH_IN = "dueDaysWithIn";
    private static final String CUSTODY_TIME_LIMIT = "custodyTimeLimit";
    private static final String IS_URGENT = "isUrgent";
    private static final String CASE_URN = "caseURN";
    private static final String HEARING_DATE = "hearingDate";
    private static final String HEARING_DAYS_WITH_IN = "hearingDaysWithIn";
    private static final String HEARING_TYPE = "hearingType";
    private static final String ASSIGNEE = "assignee";
    private static final String ACTIVITY_TYPES = "activityTypes";
    private static final String IS_CTL_ONLY = "isCtlOnly";
    private static final String CANDIDATE_GROUP = "candidateGroups";
    private static final String REGION = "region";
    private static final String ORGANISATION_ID = "organisationId";
    private static final String BUSINESS_UNIT_CODES = "businessUnitCodes";
    private static final String COURT_CODES = "courtCodes";
    private static final String IS_ASSIGNED = "isAssigned";
    public static final String WORK_QUEUE_TYPE = "workQueueTypes";
    private static final UUID organisationIdUuid = randomUUID();
    private static final String organisationIdStr = organisationIdUuid.toString();
    private static final String TASK_STATUS = "taskStatus";
    private static final String COMPLETED = "completed";
    private static final String DELETED = "deleted";
    private static final String IS_OVERDUE = "isOverdue";

    @RegisterExtension
    ProcessEngineExtension extension = ProcessEngineExtension.builder()
            .configurationResource("camunda.cfg.xml")
            .build();

    private ProcessInstance processInstance;
    private final TaskFilterService taskFilterService = new TaskFilterService();

    protected TaskService taskService;
    protected HistoryService historyService;
    protected RuntimeService runtimeService;

    @BeforeEach
    void setUp() {
        taskService = extension.getProcessEngine().getTaskService();
        historyService = extension.getProcessEngine().getHistoryService();
        runtimeService = extension.getProcessEngine().getRuntimeService();
        setField(taskFilterService, "taskService", taskService);
        setField(taskFilterService, "historyService", historyService);
        setField(taskFilterService, "runtimeService", runtimeService);
    }

    private void createProcessInstanceWithUnassignedTaskAndVariables() {
        createProcessInstance(PROCESS_KEY_VALUE);
    }

    private void createMultipleProcessInstancesWithUnassignedTaskAndVariables() {
        createProcessInstance(PROCESS_KEY_VALUE);
        createProcessInstance(PROCESS_KEY_3_VALUE);
    }

    private void createProcessInstance(String processKey){
        processInstance = runtimeService.startProcessInstanceByKey(processKey, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        runtimeService.setVariable(processInstanceId, TASK_NAME, "firstTask");
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();

        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);
        taskOne.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskOne);
        taskTwo.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(HEARING_TYPE, "hearing_type_1");
        taskOneVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(BUSINESS_UNIT_CODES, "A0FT67D");
        taskOneVariables.put(HEARING_DATE, now().toString());
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.setVariable(taskOne.getId(), HEARING_DATE, now().toString());
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");


        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskTwoVariables.put(HEARING_TYPE, "hearing_type_1");
        taskTwoVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskTwoVariables.put(COURT_CODES, "H3FT64N");
        taskOneVariables.put(HEARING_DATE, now().toString());
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.setVariable(taskTwo.getId(), HEARING_DATE, now().toString());
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");

    }

    private void createProcessInstanceWithTaskAndVariablesWithoutAssignee() {

        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        runtimeService.setVariable(processInstanceId, TASK_NAME, "firstTask");
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

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(HEARING_DATE, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(HEARING_TYPE, "hearing_type_1");
        taskOneVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(BUSINESS_UNIT_CODES, "A0FT67D");
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");

        taskService.setAssignee(taskTwo.getId(), "");
    }

    private void createProcessInstanceWithTaskAndVariables() {
        createProcessInstanceWithTaskAndVariables(PROCESS_KEY_VALUE);
    }

    private void createMultipleProcessInstanceWithTaskAndVariables() {
        createProcessInstanceWithTaskAndVariables(PROCESS_KEY_VALUE);
        createProcessInstanceWithTaskAndVariables(PROCESS_KEY_3_VALUE);
    }

    private void createProcessInstanceWithTaskAndVariables(String processKey) {

        processInstance = runtimeService.startProcessInstanceByKey(processKey, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        runtimeService.setVariable(processInstanceId, HEARING_DATE, now().toString());
        runtimeService.setVariable(processInstanceId, TASK_NAME, "firstTask");
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();

        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);
        taskOne.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskOne);
        taskTwo.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));

        taskService.setAssignee(taskOne.getId(), "testAssignee1");

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(HEARING_TYPE, "hearing_type_1");
        taskOneVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(COURT_CODES, "H3FT64");
        taskOneVariables.put(BUSINESS_UNIT_CODES, "A0FT67D");
        taskOneVariables.put(WORK_QUEUE, organisationIdStr);
        taskOneVariables.put(HEARING_DATE, now().toString());
        taskOneVariables.put(TASK_NAME, "firstTask");
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");

        taskService.setAssignee(taskTwo.getId(), "testAssignee2");
        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskTwoVariables.put(HEARING_TYPE, "hearing_type_1");
        taskTwoVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskTwoVariables.put(COURT_CODES, "H3FT64N");
        taskTwoVariables.put(HEARING_DATE, now().toString());
        taskTwoVariables.put(TASK_NAME, "secondTask");
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");
    }

    private void createProcessInstanceWithTaskComment(final boolean addComment) {
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();

        final Task taskOne = activeTasks.get(0);
        taskOne.setDueDate(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskOne);

        if (addComment) {
            taskService.createComment(taskOne.getId(), processInstanceId, "comment");
        }
        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        taskService.setAssignee(taskOne.getId(), "testAssignee1");
    }

    private void createProcessInstanceWithOverdueTask(final Boolean hasOverdue) {
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);

        if (TRUE.equals(hasOverdue)) {
            taskOne.setDueDate(from(now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskTwo.setDueDate(from(now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskService.saveTask(taskOne);
            taskService.saveTask(taskTwo);
        }

        assertThat(activeTasks, hasSize(2));
        taskService.setAssignee(taskOne.getId(), "testAssignee1");
        taskService.setAssignee(taskTwo.getId(), "testAssignee1");
    }

    private void createProcessInstanceWithDueSoonTask(final Boolean hasDueSoon) {
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);

        if (TRUE.equals(hasDueSoon)) {
            taskOne.setDueDate(from(now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskTwo.setDueDate(from(now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            taskService.saveTask(taskOne);
            taskService.saveTask(taskTwo);
        }

        assertThat(activeTasks, hasSize(2));
        taskService.setAssignee(taskOne.getId(), "testAssignee1");
        taskService.setAssignee(taskTwo.getId(), "testAssignee1");
    }

    private void createProcessInstanceWithVarriedTaskAndVariables() throws ParseException {

        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();

        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);


        final LocalDate due1 = (now().minusDays(2));
        Date dueDate1 = valueOf(due1);

        final LocalDate due2 = (now().plusDays(2));
        Date dueDate2 = valueOf(due2);
        taskOne.setDueDate(dueDate1);
        taskService.saveTask(taskOne);
        taskTwo.setDueDate(dueDate2);
        taskService.saveTask(taskTwo);

        assertThat(activeTasks, hasSize(2));
        assertThat(taskOne.getName(), is("First Task"));
        assertThat(taskTwo.getName(), is("Second Task"));

        taskService.setAssignee(taskOne.getId(), "testAssignee1");

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(HEARING_DATE, getDateAsString());
        taskOneVariables.put(HEARING_TYPE, "hearing_type_1");
        taskOneVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(BUSINESS_UNIT_CODES, "A0FT67D");
        taskOneVariables.put(ACTIVITY_TYPES, "firstTask");
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");


        taskService.setAssignee(taskTwo.getId(), "testAssignee2");
        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskTwoVariables.put(HEARING_DATE, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(HEARING_TYPE, "hearing_type_1");
        taskTwoVariables.put(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskTwoVariables.put(COURT_CODES, "H3FT64N");
        taskTwoVariables.put(ACTIVITY_TYPES, "secondTask");
        taskService.setVariablesLocal(taskTwo.getId(), taskTwoVariables);
        taskService.addCandidateGroup(taskTwo.getId(), "cps_group");

    }

    private static Date getDateAsString() throws ParseException {
        String strDate = "2023-06-09";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.parse(strDate);

    }

    @Test
    public void testFilterTasksExceptionWithoutAssignee() {
        createProcessInstanceWithTaskAndVariablesWithoutAssignee();
        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ASSIGNEE, "")
                .build();
        try {
            taskFilterService.filterTasks(filterTaskJsonObject);
        } catch (Exception e) {
            assertThat(e.toString(), Boolean.TRUE);
        }
    }

    @Test
    public void testFilterTasksExceptionWithoutOrganisation() {
        createProcessInstanceWithTaskAndVariables();
        final JsonObject filterTaskJsonObject = createObjectBuilder()
                //.add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(ASSIGNEE, "testAssignee1")
                .build();
        try {
            taskFilterService.filterTasks(filterTaskJsonObject);
        } catch (Exception e) {
            assertThat(e.toString(), Boolean.TRUE);
        }
    }

    @Test
    public void testFilterTasksUnmatchedAssignee() {
        createProcessInstanceWithTaskAndVariables();
        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(ASSIGNEE, "randomAssignee")
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables, empty());
    }

    @Test
    public void testFilterTasksFilteredTaskWithAssigneeAndOrg() {
        createProcessInstanceWithTaskAndVariables();
        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(ASSIGNEE, "testAssignee1")
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());

        assertThat(tasksWithVariables, notNullValue());
        tasksWithVariables.forEach(t -> {
            assertThat(t.getTask().getAssignee(), is("testAssignee1"));
            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));
        });
    }

    @Test
    public void testFilterTasksFilteredTaskWithAssigneeAndOrgTaskLevelFilters() {
        createProcessInstanceWithTaskAndVariables();
        final String filterDueDateString = now().plusDays(5).toString();
        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(ASSIGNEE, "testAssignee1")
                .add(DUE_DATE, filterDueDateString)
                .add(DUE_DAYS_WITH_IN, 10)
                .add(IS_ASSIGNED, TRUE)
                .add(COURT_CODES, "H3FT64")
                .add(CASE_URN, "TEST_CPS_CASE_001")
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables.size(), equalTo(1));

        for (final TaskWithVariables t : tasksWithVariables) {
            assertThat(t.getTask().getAssignee(), is("testAssignee1"));

            final List<IdentityLink> links = taskService.getIdentityLinksForTask(t.getTask().getId());
            final List<String> filterCandidateGroupList = asList("hmcts_group,random_group".split(","));
            assertThat("hmcts_group,random_group", links.stream()
                    .filter(l -> l.getType().equalsIgnoreCase("candidate"))
                    .anyMatch(g -> filterCandidateGroupList.contains(g.getGroupId())));

            assertThat("hmcts_group", TRUE);
            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));
        }
    }

    @Test
    public void testFilterTasksFilteredTaskWithDateRangeFiltersAppliedForToday() {
        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithTaskAndVariables();
            createProcessInstanceWithUnassignedTaskAndVariables();
        }

        final String filterHearingDateString = now().toString();
        final String filterDueDateString = now().toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(DUE_DATE, filterDueDateString)
                .add(DUE_DAYS_WITH_IN, 0)
                .add(HEARING_DATE, filterHearingDateString)
                .add(HEARING_DAYS_WITH_IN, 0)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());

        for (final TaskWithVariables t : tasksWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));

            final List<String> hearingDate = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(HEARING_DATE))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(hearingDate, notNullValue());
        }
    }


    @Test
    public void testFilterTasksFilteredTaskWithDateRangeFiltersAppliedForPastDay() throws ParseException {
        for (int i = 0; i < 3; i++) {
            createProcessInstanceWithVarriedTaskAndVariables();
        }

        final String filterHearingDateString = now().toString();
        final String filterDueDateString = now().minusDays(2).toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(DUE_DATE, filterDueDateString)
                .add(DUE_DAYS_WITH_IN, 0)
                .add(HEARING_DATE, filterHearingDateString)
                .add(HEARING_DAYS_WITH_IN, 0)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());

        for (final TaskWithVariables t : tasksWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));

            final Date dueDate = t.getTask().getDueDate();
            final Date filterDueDate = from(LocalDate.parse(filterDueDateString).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            assertThat(filterDueDate, is(dueDate));

            final List<Date> hearingDate = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(HEARING_DATE))
                    .map(v -> (Date) v.getValue())
                    .collect(toList());
            assertThat(hearingDate, notNullValue());
        }
    }

    @Test
    public void testFilterTasksFilteredTaskWithDateRangeFiltersAppliedForThisWeek() {
        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithTaskAndVariables();
            createProcessInstanceWithUnassignedTaskAndVariables();
        }

        final String filterHearingDateString = now().toString();
        final String filterDueDateString = now().toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(DUE_DATE, filterDueDateString)
                .add(DUE_DAYS_WITH_IN, 7)
                .add(HEARING_DATE, filterHearingDateString)
                .add(HEARING_DAYS_WITH_IN, 7)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());

        for (final TaskWithVariables t : tasksWithVariables) {
            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));
            final List<String> hearingDate = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(HEARING_DATE))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(hearingDate, notNullValue());
        }
    }


    // Test case for filtering when not in the specified date range, should not include any task
    @Test
    public void testFilteredTaskIfNotInDateRangeFiltersAppliedForThisWeek() throws ParseException {

        for (int i = 0; i < 3; i++) {
            createProcessInstanceWithVarriedTaskAndVariables();
        }

        final String filterHearingDateString = now().toString();
        final String filterDueDateString = now().minusDays(15).toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(DUE_DATE, filterDueDateString)
                .add(DUE_DAYS_WITH_IN, 7)
                .add(HEARING_DATE, filterHearingDateString)
                .add(HEARING_DAYS_WITH_IN, 7)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> taskWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(taskWithVariables.size(), is(0));

        for (final TaskWithVariables t : taskWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));

            final List<Date> hearingDate = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(HEARING_DATE))
                    .map(v -> (Date) v.getValue())
                    .collect(toList());
            assertThat(hearingDate, notNullValue());
        }
    }

    @Test
    public void testFilterTasksFilteredTaskWithDateRangeFiltersAppliedForPastNoResult() {
        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithTaskAndVariables();
            createProcessInstanceWithUnassignedTaskAndVariables();
        }

        final String filterHearingDateString = now().minusDays(15).toString();
        final String filterDueDateString = now().minusDays(10).toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(DUE_DATE, filterDueDateString)
                .add(DUE_DAYS_WITH_IN, 9)
                .add(HEARING_DATE, filterHearingDateString)
                .add(HEARING_DAYS_WITH_IN, 14)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables, empty());
    }

    @Test
    public void testFilterTasksFilteredTaskWithDateRangeFiltersAppliedForPastCurrentDayResult() {
        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithTaskAndVariables();
            createProcessInstanceWithUnassignedTaskAndVariables();
        }

        final String filterHearingDateString = now().toString();
        final String filterDueDateString = now().minusDays(10).toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(DUE_DATE, filterDueDateString)
                .add(DUE_DAYS_WITH_IN, 10)
                .add(HEARING_DATE, filterHearingDateString)
                .add(HEARING_DAYS_WITH_IN, 2)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables.size(), is(80));

        for (final TaskWithVariables t : tasksWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .toList();
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));
            //Only todays due tasks will be pulled
            final Date dueDate = t.getTask().getDueDate();
            assertThat(dueDate.toInstant().truncatedTo(ChronoUnit.DAYS), is(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()).toInstant().truncatedTo(ChronoUnit.DAYS)));

            final List<String> hearingDate = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(HEARING_DATE))
                    .map(v -> (String)v.getValue())
                    .toList();
            //Only todays hearing date tasks will be pulled
            assertThat(hearingDate, notNullValue());
            assertThat(hearingDate.get(0), is(filterHearingDateString));
        }
    }


    @Test
    public void testFilterTasksFilteredTaskWithoutDateRangeFiltersForDueDate() {
        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithTaskAndVariables();
            createProcessInstanceWithUnassignedTaskAndVariables();
        }

        final String filterDueDateString = now().toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(DUE_DATE, filterDueDateString)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables.size(), is(80));

        for (final TaskWithVariables t : tasksWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));
            //Only todays due tasks will be pulled
            final Date dueDate = t.getTask().getDueDate();
            assertThat(dueDate.toInstant().truncatedTo(ChronoUnit.DAYS), is(Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()).toInstant().truncatedTo(ChronoUnit.DAYS)));
        }
    }

    @Test
    public void testFilterTasksFilteredTaskWithoutDateRangeFiltersForHearingDate() {
        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithTaskAndVariables();
            createProcessInstanceWithUnassignedTaskAndVariables();
        }

        final String filterHearingDateString = now().toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(HEARING_DATE, filterHearingDateString)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables.size(), is(greaterThan(0)));
        for (final TaskWithVariables t : tasksWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));

            final List<String> hearingDate = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(HEARING_DATE))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            //Only todays hearing date tasks will be pulled
            assertThat(hearingDate, notNullValue());
            assertThat(hearingDate.get(0), is(filterHearingDateString));
        }
    }


    @Test
    public void testFilterTasksFilteredTaskWithAllFilters() {
        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithTaskAndVariables();
            createProcessInstanceWithUnassignedTaskAndVariables();
        }

        final String filterHearingDateString = now().plusDays(20).toString();
        final String filterDueDateString = now().plusDays(5).toString();

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .add(ASSIGNEE, "testAssignee1")
                .add(DUE_DATE, filterDueDateString)
                .add(DUE_DAYS_WITH_IN, 10)
                .add(IS_ASSIGNED, TRUE)
                .add(HEARING_DATE, filterHearingDateString)
                .add(HEARING_DAYS_WITH_IN, 40)
                .add(CASE_URN, "TEST_CPS_CASE_001")
                .add(BUSINESS_UNIT_CODES, "A0FT67D,GH9034FB,KS342BV5")
                .add(ACTIVITY_TYPES, "firstTask,secondTask")
                .add(IS_CTL_ONLY, true)
                .add(IS_URGENT, true)
                .add(WORK_QUEUE_TYPE, "e7be77a9-9ac5-431d-a23b-2d750e441b75")
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());

        for (final TaskWithVariables t : tasksWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));

            assertThat(t.getTask().getAssignee(), is("testAssignee1"));

            final List<IdentityLink> links = taskService.getIdentityLinksForTask(t.getTask().getId());
            final List<String> filterCandidateGroupList = asList("hmcts_group,random_group".split(","));
            assertThat("hmcts_group,random_group", links.stream()
                    .filter(l -> l.getType().equalsIgnoreCase("candidate"))
                    .anyMatch(g -> filterCandidateGroupList.contains(g.getGroupId())));

            final List<Date> hearingDate = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(HEARING_DATE))
                    .map(v -> (Date) v.getValue())
                    .collect(toList());
            assertThat(hearingDate, notNullValue());
            final List<String> caseUrnList = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(CASE_URN))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(caseUrnList, notNullValue());
            assertThat("TEST_CPS_CASE_001", is(caseUrnList.get(0)));

            List<String> businessUnitCodeList = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(BUSINESS_UNIT_CODES))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(businessUnitCodeList, notNullValue());

            businessUnitCodeList = asList(businessUnitCodeList.get(0).split(","));
            List<String> filterbusinessUnitCodes = asList("A0FT67D,GH9034FB,KS342BV5".split(","));
            assertThat("hmcts_group,random_group", businessUnitCodeList.stream()
                    .anyMatch(filterbusinessUnitCodes::contains));

            final List<String> filterActivityTypeList = asList("firstTask,secondTask".split(","));
            assertThat(t.getTask().getTaskDefinitionKey(), filterActivityTypeList.contains(t.getTask().getTaskDefinitionKey()));

            final List<Date> custodyTimeLimitList = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(CUSTODY_TIME_LIMIT))
                    .map(v -> (Date) v.getValue())
                    .collect(toList());
            assertThat(custodyTimeLimitList, notNullValue());
            assertThat(custodyTimeLimitList.get(0), is(notNullValue()));

            final List<Boolean> isUrgentList = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(IS_URGENT))
                    .map(v -> (Boolean) v.getValue())
                    .collect(toList());
            assertThat(isUrgentList, notNullValue());
            assertThat(isUrgentList.get(0), is(TRUE));
        }
    }


    @Test
    void testFilterTasksFilteredTaskWithActivityFiltersAndWithTestAssignee2() {

        for (int i = 0; i < 20; i++) {
            createProcessInstanceWithTaskAndVariables();
            createProcessInstanceWithUnassignedTaskAndVariables();
        }

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ASSIGNEE, "testAssignee1")
                .add(ACTIVITY_TYPES, "firstTask,secondTask")
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables, hasSize(20));

        for (final TaskWithVariables t : tasksWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));

            assertThat(t.getTask().getAssignee(), is("testAssignee1"));

            final List<String> caseUrnList = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(CASE_URN))
                    .map(v -> (String) v.getValue())
                    .collect(toList());
            assertThat(caseUrnList, notNullValue());
            assertThat("TEST_CPS_CASE_001", is(caseUrnList.get(0)));
            assertThat(t.getTask().getTaskDefinitionKey(), is("firstTask"));
        }
    }

    @Test
    void testFilterUserTasks_filteredByActivityTypeAndNoAssignee() {

        for (int i = 0; i < 10; i++) {
            createMultipleProcessInstanceWithTaskAndVariables();
            createMultipleProcessInstancesWithUnassignedTaskAndVariables();
        }

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ACTIVITY_TYPES, "firstTask,fourthTask")
                .add(IS_ASSIGNED, false)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables, hasSize(20));

        final List<String> taskDefinitionKeys = tasksWithVariables.stream()
                .map(t -> t.getTask().getTaskDefinitionKey())
                .distinct()
                .toList();

        assertThat(taskDefinitionKeys, containsInAnyOrder("firstTask", "fourthTask"));

        for (final TaskWithVariables t : tasksWithVariables) {

            final List<String> organisationId = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(ORGANISATION_ID))
                    .map(v -> (String) v.getValue())
                    .toList();
            assertThat(organisationId, notNullValue());
            assertThat("e7be77a9-9ac5-431d-a23b-2d750e441b75", is(organisationId.get(0)));

            assertThat(t.getTask().getAssignee(), nullValue());

            final List<String> caseUrnList = t.getVariableList().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(CASE_URN))
                    .map(v -> (String) v.getValue())
                    .toList();

            assertThat(caseUrnList, notNullValue());
            assertThat("TEST_CPS_CASE_001", is(caseUrnList.get(0)));
        }
    }

    @Test
    void testFilterMultipleUserTasks_filteredByActivityTypeAndNoAssignee() {
        for (int i = 0; i < 10; i++) {
            createMultipleProcessInstanceWithTaskAndVariables();
            createMultipleProcessInstancesWithUnassignedTaskAndVariables();
        }

        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ACTIVITY_TYPES, "firstTask,secondTask,fourthTask,fifthTask")
                .add(IS_ASSIGNED, true)
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables, hasSize(40));

        final List<String> taskDefinitionKeys = tasksWithVariables.stream()
                .map(t -> t.getTask().getTaskDefinitionKey())
                .distinct()
                .toList();

        assertThat(taskDefinitionKeys, containsInAnyOrder("firstTask", "secondTask", "fourthTask", "fifthTask"));
    }

    @Test
    public void testFilterTasksFilteredTaskWithoutComment() {
        testFilterTasksFilteredTaskComment(false);
    }

    @Test
    public void testFilterTasksFilteredTaskWithOverdueIsTRUE() {
        testFilterTasksFilteredTaskWithOverdue(true);
    }

    @Test
    public void testFilterTasksFilteredTaskWithOverdueIsFALSE() {
        testFilterTasksFilteredTaskWithOverdue(false);
    }

    @Test
    public void testFilterTasksFilteredTaskWithOverdueIsNULL() {
        testFilterTasksFilteredTaskWithOverdue(null);
    }


    @Test
    public void testFilterTasksFilteredTaskWithDueSoonIsTRUE() {
        testFilterTasksFilteredTaskWithDueSoon(true);
    }

    @Test
    public void testFilterTasksFilteredTaskWithDueSoonIsFALSE() {
        testFilterTasksFilteredTaskWithDueSoon(false);
    }

    @Test
    public void testFilterTasksFilteredTaskWithDueSoonIsNULL() {
        testFilterTasksFilteredTaskWithDueSoon(null);
    }

    public void testFilterTasksFilteredTaskComment(final boolean addComment) {
        createProcessInstanceWithTaskComment(addComment);
        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ASSIGNEE, "testAssignee1")
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());

        tasksWithVariables.forEach(t -> {
            assertThat(t.getTask().getAssignee(), is("testAssignee1"));
            assertThat(t.getCommentCount(), is(addComment ? 1 : 0));
        });
    }

    @Test
    public void testTaskWithDeletedStatus() {

        createProcessInstanceWithTaskAndVariablesForDeletedAndCompletedTask(DELETED);
        final JsonObject filterTaskJsonObject = createObjectBuilder().add(ASSIGNEE, "testAssignee1").add(TASK_STATUS, "deleted").build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables.size(), is(1));
        assertThat(tasksWithVariables.get(0).getTask().getName(), is("First Task"));
    }

    @Test
    public void testTaskWithCompletedStatus() {

        createProcessInstanceWithTaskAndVariablesForDeletedAndCompletedTask(COMPLETED);
        final JsonObject filterTaskJsonObject = createObjectBuilder()
                .add(ASSIGNEE, "testAssignee1")
                .add(TASK_STATUS, "completed")
                .add(CASE_TAG, "samplecasetag")
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables.size(), is(1));
        assertThat(tasksWithVariables.get(0).getTask().getName(), is("First Task"));

    }

    @Test
    public void testEmptyTasksList() {

        createProcessInstanceWithTaskAndVariablesForDeletedAndCompletedTask(COMPLETED);
        final JsonObject filterTaskJsonObject = createObjectBuilder().add(ASSIGNEE, "testAssignee1")
                .add(TASK_STATUS, "completed")
                .add(CANDIDATE_GROUP, "hmcts_group,random_group")
                .build();

        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables.size(), is(0));

    }

    private void createProcessInstanceWithTaskAndVariablesForDeletedAndCompletedTask(String value) {

        final Date hearingDate = convertToDate("2022-05-23T10:00:00.000Z");
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, organisationIdStr);
        runtimeService.setVariable(processInstanceId, CASE_URN, "TEST_CPS_CASE_001");
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        final Task taskOne = activeTasks.get(0);
        final Task taskTwo = activeTasks.get(1);

        assertThat(activeTasks, hasSize(2));

        taskOne.setDueDate(hearingDate);
        taskService.saveTask(taskOne);
        taskService.setAssignee(taskOne.getId(), "testAssignee1");
        if (DELETED.equalsIgnoreCase(value)) {
            taskService.setVariableLocal(taskOne.getId(), DELETED, DELETED);
        } else {
            taskService.setVariableLocal(taskOne.getId(), COMPLETED, COMPLETED);
        }

        final Map<String, Object> taskOneVariables = new HashMap<>();
        taskOneVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskOneVariables.put(IS_URGENT, TRUE);
        taskOneVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskOneVariables.put(HEARING_DATE, hearingDate);
        taskOneVariables.put(HEARING_TYPE, "hearing_type_1");
        taskOneVariables.put(REGION, "HMCTS");
        taskOneVariables.put(CASE_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        taskOneVariables.put(TASK_TYPE, "taskType1");
        taskOneVariables.put(ACTIVITY_TYPES, "firstTask");
        taskOneVariables.put(CASE_TAG, "samplecasetag");
        taskOneVariables.put(ORGANISATION_ID, organisationIdStr);
        taskService.setVariablesLocal(taskOne.getId(), taskOneVariables);
        taskService.addCandidateGroup(taskOne.getId(), "hmcts_group");
        taskService.addCandidateGroup(taskOne.getId(), "random_hmcts_group");

        final Map<String, Object> taskTwoVariables = new HashMap<>();
        taskTwoVariables.put(CUSTODY_TIME_LIMIT, Date.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()));
        taskTwoVariables.put(IS_URGENT, TRUE);
        taskTwoVariables.put(CASE_URN, "TEST_CPS_CASE_001");
        taskTwoVariables.put(HEARING_DATE, hearingDate);
        taskTwoVariables.put(HEARING_TYPE, "hearing_type_1");
        taskTwoVariables.put(REGION, "CPS");
        taskTwoVariables.put(ACTIVITY_TYPES, "secondTask");
        taskTwoVariables.put(CASE_ID, organisationIdStr);
        taskTwoVariables.put(TASK_TYPE, "taskType2");
        taskTwoVariables.put(WORK_QUEUE, organisationIdStr);
        taskTwoVariables.put(ORGANISATION_ID, organisationIdStr);
        taskService.setVariablesLocal(taskTwo.getId(), taskOneVariables);

        if (DELETED.equalsIgnoreCase(value)) {
            final Task updatedTask = taskService.createTaskQuery().taskId(taskOne.getId()).singleResult();
            runtimeService.suspendProcessInstanceById(updatedTask.getProcessInstanceId());
            runtimeService.deleteProcessInstance(updatedTask.getProcessInstanceId(), "deleted");
            taskService.deleteTask(taskOne.getId(), "deleted");
        } else {
            taskService.complete(taskOne.getId());
        }

    }

    public void testFilterTasksFilteredTaskWithOverdue(final Boolean hasOverdue) {
        createProcessInstanceWithOverdueTask(hasOverdue);
        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(ASSIGNEE, "testAssignee1");

        if (TRUE.equals(hasOverdue)) {
            jsonObjectBuilder.add(IS_OVERDUE, hasOverdue);
        }

        final JsonObject filterTaskJsonObject = jsonObjectBuilder.build();
        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables, hasSize(TRUE.equals(hasOverdue) ? 1 : 2));
    }

    public void testFilterTasksFilteredTaskWithDueSoon(final Boolean hasDueSoon) {
        createProcessInstanceWithDueSoonTask(hasDueSoon);
        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(ASSIGNEE, "testAssignee1");

        if (TRUE.equals(hasDueSoon)) {
            jsonObjectBuilder.add(IS_DUE_SOON, hasDueSoon);
        }

        final JsonObject filterTaskJsonObject = jsonObjectBuilder.build();
        final FilteredTasks filteredTasks = taskFilterService.filterTasks(filterTaskJsonObject);
        assertThat(filteredTasks, notNullValue());

        final List<TaskWithVariables> tasksWithVariables = filteredTasks.getTaskWithVariables();
        assertThat(tasksWithVariables, notNullValue());
        assertThat(tasksWithVariables, hasSize(TRUE.equals(hasDueSoon) ? 1 : 2));

    }

    private WorkflowTaskType createWorkflowTaskType(final String taskName) {
        return WorkflowTaskType.builder()
                .taskName(taskName)
                .displayName(taskName)
                .build();
    }
}