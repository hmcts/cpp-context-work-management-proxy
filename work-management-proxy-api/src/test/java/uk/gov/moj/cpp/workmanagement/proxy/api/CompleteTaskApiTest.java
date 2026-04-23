package uk.gov.moj.cpp.workmanagement.proxy.api;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LAST_UPDATED_BY_ID;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.UserService;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, ProcessEngineExtension.class})
@Deployment(resources = {"sample-test-process.bpmn"})
public class CompleteTaskApiTest {

    private static final String DELETION_REASON = "deletionReason";
    private static final String COMPLETION_REASON = "completionReason";
    private static final String CANCELLATION_REASON = "cancellationReason";

    public static final UUID USER_ID = randomUUID();

    @RegisterExtension
    ProcessEngineExtension extension = ProcessEngineExtension.builder()
            .configurationResource("camunda.cfg.xml")
            .build();

    private ProcessInstance processInstance;

    private CompleteTaskApi deleteTaskApi = new CompleteTaskApi();

    @Mock
    private UserService userService;

    private String userDetails = "Jane Doe";
    protected TaskService taskService;
    protected RuntimeService runtimeService;
    protected HistoryService historyService;

    protected ProcessEngine processEngine;

    @BeforeEach
    public void setUp() throws Exception {
        taskService = extension.getProcessEngine().getTaskService();
        runtimeService = extension.getProcessEngine().getRuntimeService();
        historyService = extension.getProcessEngine().getHistoryService();
        processEngine=extension.getProcessEngine();
        setField(deleteTaskApi, "taskService", taskService);
        setField(deleteTaskApi, "userService", userService);
        setField(deleteTaskApi, "runtimeService", runtimeService);
        setField(deleteTaskApi, "processEngine", processEngine);
    }

   @Test
    public void testDeleteTask_TaskPresentAndActive() {
        processInstance = runtimeService.startProcessInstanceByKey("sample_test_custom_task_process", randomUUID().toString());

        final String processInstanceId = processInstance.getProcessInstanceId();
        when(userService.getUserDetails(USER_ID.toString())).thenReturn(userDetails);
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasks, hasSize(2));
        assertThat(activeTasks.get(0).getName(), is("First Task"));
        assertThat(activeTasks.get(1).getName(), is("Second Task"));

        final String firstTaskId = activeTasks.get(0).getId();
        final String deletionReasonValue = "no longer needed";

        deleteTaskApi.completeTask(getDeleteJsonEnvelope(firstTaskId, deletionReasonValue));
        final List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).finished().list();
        final List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .taskIdIn(historicTaskInstances.get(0).getId())
                .list();
        assertTrue(historicVariableInstances.stream()
                .anyMatch(hv -> hv.getName().equalsIgnoreCase(DELETION_REASON) && hv.getValue().toString().equalsIgnoreCase(deletionReasonValue)));
        assertTrue(historicVariableInstances.stream()
                .anyMatch(hv -> hv.getName().equalsIgnoreCase(LAST_UPDATED_BY_ID) && hv.getValue().toString().equalsIgnoreCase(USER_ID.toString())));

    }
    @Test
    public void testDeleteTask_withCompletionReason() {
        processInstance = runtimeService.startProcessInstanceByKey("sample_test_custom_task_process", randomUUID().toString());

        final String processInstanceId = processInstance.getProcessInstanceId();
        when(userService.getUserDetails(USER_ID.toString())).thenReturn(userDetails);
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasks, hasSize(2));
        assertThat(activeTasks.get(0).getName(), is("First Task"));
        assertThat(activeTasks.get(1).getName(), is("Second Task"));

        final String firstTaskId = activeTasks.get(0).getId();
        final String completionReason = "no longer needed";

        deleteTaskApi.completeTask(getCompleteJsonEnvelope(firstTaskId, completionReason));
        final List<Task> activeTasksAfterDeletion = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasksAfterDeletion, hasSize(2));
        assertThat(activeTasksAfterDeletion.get(0).getName(), is("Second Task"));
        assertThat(activeTasksAfterDeletion.get(1).getName(), is("Third Task"));

    }

    @Test
    public void testDeleteTask_withCancelReason() {
        processInstance = runtimeService.startProcessInstanceByKey("sample_test_custom_task_process", randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        when(userService.getUserDetails(USER_ID.toString())).thenReturn(userDetails);
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasks, hasSize(2));
        assertThat(activeTasks.get(0).getName(), is("First Task"));
        assertThat(activeTasks.get(1).getName(), is("Second Task"));

        final String firstTaskId = activeTasks.get(0).getId();
        final String cancelReason = "no longer needed";

        deleteTaskApi.completeTask(getCancelJsonEnvelope(firstTaskId, cancelReason));
        final List<Task> activeTasksAfterDeletion = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasksAfterDeletion, hasSize(2));
        assertThat(activeTasksAfterDeletion.get(0).getName(), is("Second Task"));
        assertThat(activeTasksAfterDeletion.get(1).getName(), is("Third Task"));

    }

    @Test
    public void testDeleteTask_withoutAnyReason() {
        processInstance = runtimeService.startProcessInstanceByKey("sample_test_custom_task_process", randomUUID().toString());

        final String processInstanceId = processInstance.getProcessInstanceId();
        when(userService.getUserDetails(USER_ID.toString())).thenReturn(userDetails);
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasks, hasSize(2));
        assertThat(activeTasks.get(0).getName(), is("First Task"));
        assertThat(activeTasks.get(1).getName(), is("Second Task"));

        final String firstTaskId = activeTasks.get(0).getId();
        final String noReason=null;

        deleteTaskApi.completeTask(getJsonEnvelopeForNoReasonPassed(firstTaskId, noReason));
        final List<Task> activeTasksAfterDeletion = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasksAfterDeletion, hasSize(2));
        assertThat(activeTasksAfterDeletion.get(0).getName(), is("Second Task"));
        assertThat(activeTasksAfterDeletion.get(1).getName(), is("Third Task"));

    }


    @Test
    public void testDeleteTask_TaskNotPresentAndCannotBeDeleted() {
        final String deletionReasonValue = "no longer needed";
        final String randomTaskId = randomUUID().toString();

        assertThrows(BadRequestException.class,
                () -> deleteTaskApi.completeTask(getCancelJsonEnvelope(randomTaskId, deletionReasonValue)));
    }

    private JsonEnvelope getDeleteJsonEnvelope(final String firstTaskId, final String deletionReason) {
        final JsonObject payload = createObjectBuilder()
                .add("taskId", firstTaskId)
                .add(DELETION_REASON, deletionReason)
                .build();
        return envelopeFrom(metadataBuilder().withName("complete-task").withId(randomUUID()).withUserId(USER_ID.toString()), payload);
    }

    @Test
    public void testCompleteTask_TaskPresentAndActive() {
        processInstance = runtimeService.startProcessInstanceByKey("sample_test_custom_task_process", randomUUID().toString());

        final String processInstanceId = processInstance.getProcessInstanceId();
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasks, hasSize(2));
        assertThat(activeTasks.get(0).getName(), is("First Task"));
        assertThat(activeTasks.get(1).getName(), is("Second Task"));

        final String firstTaskId = activeTasks.get(0).getId();
        final String completionReasonValue = "complete";

        deleteTaskApi.completeTask(getCompleteJsonEnvelope(firstTaskId, completionReasonValue));

        final List<Task> activeTasksAfterDeletion = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasksAfterDeletion, hasSize(2));
        assertThat(activeTasksAfterDeletion.get(0).getName(), is("Second Task"));
        assertThat(activeTasksAfterDeletion.get(1).getName(), is("Third Task"));

        final List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).finished().list();
        final List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .taskIdIn(historicTaskInstances.get(0).getId())
                .list();
        assertTrue(historicVariableInstances.stream()
                .anyMatch(hv -> hv.getName().equalsIgnoreCase(COMPLETION_REASON) && hv.getValue().toString().equalsIgnoreCase(completionReasonValue)));
        assertTrue(historicVariableInstances.stream()
                .anyMatch(hv -> hv.getName().equalsIgnoreCase(LAST_UPDATED_BY_ID) && hv.getValue().toString().equalsIgnoreCase(USER_ID.toString())));

    }

    @Test
    public void testCancelTask_TaskPresentAndActive() {
        processInstance = runtimeService.startProcessInstanceByKey("sample_test_custom_task_process", randomUUID().toString());

        final String processInstanceId = processInstance.getProcessInstanceId();
        final List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasks, hasSize(2));
        assertThat(activeTasks.get(0).getName(), is("First Task"));
        assertThat(activeTasks.get(1).getName(), is("Second Task"));

        final String firstTaskId = activeTasks.get(0).getId();
        final String deletionReasonValue = "cancel";

        deleteTaskApi.completeTask(getCancelJsonEnvelope(firstTaskId, deletionReasonValue));

        final List<Task> activeTasksAfterDeletion = taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
        assertThat(activeTasksAfterDeletion, hasSize(2));
        assertThat(activeTasksAfterDeletion.get(0).getName(), is("Second Task"));
        assertThat(activeTasksAfterDeletion.get(1).getName(), is("Third Task"));

        final List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).finished().list();
        final List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .taskIdIn(historicTaskInstances.get(0).getId())
                .list();
        assertTrue(historicVariableInstances.stream()
                .anyMatch(hv -> hv.getName().equalsIgnoreCase(CANCELLATION_REASON) && hv.getValue().toString().equalsIgnoreCase(deletionReasonValue)));
        assertTrue(historicVariableInstances.stream()
                .anyMatch(hv -> hv.getName().equalsIgnoreCase(LAST_UPDATED_BY_ID) && hv.getValue().toString().equalsIgnoreCase(USER_ID.toString())));

    }

    private JsonEnvelope getCompleteJsonEnvelope(final String firstTaskId, final String completionReason) {
        final JsonObject payload = createObjectBuilder()
                .add("taskId", firstTaskId)
                .add(COMPLETION_REASON, completionReason)
                .build();
        return envelopeFrom(metadataBuilder().withName("complete-task").withId(randomUUID()).withUserId(USER_ID.toString()), payload);
    }

    private JsonEnvelope getCancelJsonEnvelope(final String firstTaskId, final String cancellationReason) {
        final JsonObject payload = createObjectBuilder()
                .add("taskId", firstTaskId)
                .add(CANCELLATION_REASON, cancellationReason)
                .build();
        return envelopeFrom(metadataBuilder().withName("complete-task").withId(randomUUID()).withUserId(USER_ID.toString()), payload);
    }

    private JsonEnvelope getJsonEnvelopeForNoReasonPassed(final String firstTaskId, final String noReasonProvided) {
        final JsonObject payload = createObjectBuilder()
                .add("taskId", firstTaskId)
                .add(CANCELLATION_REASON, JsonValue.NULL)
                .add(DELETION_REASON,JsonValue.NULL)
                .add(COMPLETION_REASON,JsonValue.NULL)
                .build();
        return envelopeFrom(metadataBuilder().withName("complete-task").withId(randomUUID()).withUserId(USER_ID.toString()), payload);
    }
}