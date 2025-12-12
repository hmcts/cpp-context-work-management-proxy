package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.camunda.bpm.engine.impl.json.JsonTaskQueryConverter.TASK_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.mapper.IdentityLinkMapper.IDENTITY_LINK_MAPPER;
import static uk.gov.moj.cpp.workmanagement.proxy.api.mapper.TaskMapper.TASK_MAPPER;
import static uk.gov.moj.cpp.workmanagement.proxy.api.mapper.VariableInstanceMapper.VARIABLE_INSTANCE_MAPPER;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ASSIGNEE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ASSIGN_TO_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANCELLATION_REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANDIDATE_GROUPS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_URN;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COMMENT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COMPLETION_REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COURT_CODES;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CUSTOM_TASK_TYPE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DEFENDANTS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DELETION_REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DUE_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.HEARING_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_DEFERRABLE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_DELETABLE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_URGENT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LAST_UPDATED_BY_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LAST_UPDATED_BY_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LIMIT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.NOTE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.NOTE_LAST_UPDATED_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.NOTE_LAST_UPDATED_USER;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.OFFSET;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.PREVIOUS_DUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.PREVIOUS_WORK_QUEUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_DISPLAY_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_TYPE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_TYPE_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_VARIABLES_JSON_STRING;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.VAR_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils.convertToDate;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils.convertToLocalDate;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.exception.InputDataException;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.ActivitySummary;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.FilteredTasks;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowCustomTaskType;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowTaskType;
import uk.gov.moj.cpp.workmanagement.proxy.api.provider.ActivitySummaryProvider;
import uk.gov.moj.cps.workmanagement.proxy.api.CreateGenericTask;
import uk.gov.moj.cps.workmanagement.proxy.api.Defendant;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CamundaJavaApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaJavaApiService.class);

    @SuppressWarnings("squid:S00115")
    private static final String genericTaskProcessKey = "generic_task_process";

    private static final String ASSIGNEE_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE = "assignee cannot be null!";
    private static final String TASK_ID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE = "taskId cannot be null!";
    private static final String TASK_NOT_FOUND_BY_TASK_ID = "the task not found by the taskId";
    private static final String TASK_TYPE_ID_NOT_FOUND = "taskTypeId not found in the task variables";
    private static final String GENERIC_TASK_VARIABLES_JSON_STRING = "genericTaskVariablesJsonString";
    private static final String USER_ID = "userId";
    private static final String CUSTOM_PROCESS_SUFFIX = "_custom_task_process";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private ProcessEngine processEngine;

    @Inject
    private TaskService taskService;

    @Inject
    private HistoryService historyService;

    @Inject
    private TaskFilterService taskFilterService;

    @Inject
    private ObjectMapperProducer objectMapperProducer;

    @Inject
    private UserService userService;

    @Inject
    private ReferenceDataService referenceDataService;

    public FilteredTasks queryTasksAssigneeWithVariables(JsonEnvelope envelope) {

        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        final int offset = jsonObject.getInt(OFFSET, 0);
        final int limit = jsonObject.getInt(LIMIT, 50);
        if (!jsonObject.containsKey(ASSIGNEE)) {
            throw new IllegalArgumentException(ASSIGNEE_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE);
        }
        final String assigneeUserId = jsonObject.getString(ASSIGNEE);

        LOGGER.info("Received the query for list of tasks assigned to assignee : {}", assigneeUserId);
        return taskFilterService.filterTasks(jsonObject, offset, limit);
    }

    public TaskWithVariables assignTask(final JsonEnvelope envelope) {
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE));
        final JsonObject inputJson = envelope.payloadAsJsonObject();
        final String taskId = inputJson.getString(TASK_ID, "");
        final String assignedUserId = inputJson.getString(USER_ID, "");

        final TaskQuery taskQuery = taskService.createTaskQuery();
        taskQuery.taskId(taskId);

        taskService.setVariableLocal(taskId, LAST_UPDATED_BY_ID, userId);
        LOGGER.info("Fetching user details of user : {}", userId);
        final String userDetails = userService.getUserDetails(userId);
        if (isNotEmpty(userDetails)) {
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userDetails);
        } else {
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userId);
        }

        if (isNotEmpty(assignedUserId)) {
            taskService.setAssignee(taskId, assignedUserId);
            taskService.setVariableLocal(taskId, ASSIGNEE, assignedUserId);
            LOGGER.info("Fetching user details of assignee : {}", assignedUserId);
            final String assigneeDetails = userService.getUserDetails(assignedUserId);
            if (isNotEmpty(assigneeDetails)) {
                taskService.setVariableLocal(taskId, ASSIGN_TO_NAME, assigneeDetails);
            } else {
                taskService.setVariableLocal(taskId, ASSIGN_TO_NAME, assignedUserId);
            }
        } else {
            taskService.setAssignee(taskId, null);
            taskService.setVariableLocal(taskId, ASSIGNEE, null);
            taskService.setVariableLocal(taskId, ASSIGN_TO_NAME, null);
        }

        return new TaskWithVariables(TASK_MAPPER.taskToTaskDto(taskQuery.orderByTaskName().asc().list().get(0)));
    }

    public TaskWithVariables createCustomTaskAndVariables(final JsonEnvelope envelope) {

        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        LOGGER.info("----------------create-custom-task command payload : {}", jsonObject);
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE));
        final String assignedUserId = jsonObject.getString(ASSIGNEE, EMPTY);
        final String taskName = jsonObject.getString(TASK_NAME);
        final String taskType = jsonObject.getString(TASK_TYPE);
        final String comment = jsonObject.getString(COMMENT, null);
        final String userDetails = userService.getUserDetails(userId);
        final String hearingDate = jsonObject.getString(HEARING_DATE, "");
        final String displayName = jsonObject.getString(TASK_DISPLAY_NAME, "");

        final ProcessInstance customTaskProcessInstance = runtimeService
                .createProcessInstanceByKey(getTaskDefinitionKey(taskName))
                .setVariable(TASK_VARIABLES_JSON_STRING, jsonObject.toString())
                .setVariable(USER_ID, userId)
                .setVariable(CUSTOM_TASK_TYPE, taskName)
                .setVariable(TASK_NAME, displayName)
                .setVariable(TASK_TYPE, taskType)
                .setVariable(LAST_UPDATED_BY_ID, userId)
                .setVariable(LAST_UPDATED_BY_NAME, userDetails)
                .setVariable(PREVIOUS_DUE, jsonObject.getString(DUE, EMPTY))
                .execute();
        LOGGER.info("custom task processInstance id : {}", customTaskProcessInstance.getProcessInstanceId());

        final List<Task> createdTasks = taskService.createTaskQuery().processInstanceId(customTaskProcessInstance.getProcessInstanceId()).list();
        final Task createdTask = createdTasks.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Task not created !!"));

        if (isNotEmpty(comment)) {
            taskService.createComment(createdTask.getId(), customTaskProcessInstance.getProcessInstanceId(), comment);
        }

        if (isNotEmpty(assignedUserId)) {
            final String taskId = createdTask.getId();
            final String assigneeDetails = userService.getUserDetails(assignedUserId);
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_ID, userId);
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userDetails);
            taskService.setVariableLocal(taskId, ASSIGN_TO_NAME, assigneeDetails);
            taskService.setAssignee(taskId, assignedUserId);
        }
        taskService.setVariableLocal(createdTask.getId(), HEARING_DATE, hearingDate);

        final Task updatedTask = taskService.createTaskQuery().taskId(createdTask.getId()).singleResult();
        final TaskWithVariables taskWithVariables = new TaskWithVariables(TASK_MAPPER.taskToTaskDto(updatedTask));
        List<VariableInstance> variableInstances = VARIABLE_INSTANCE_MAPPER.variableInstancesToVariableInstancesDto(runtimeService.createVariableInstanceQuery().taskIdIn(updatedTask.getId()).list());
        variableInstances.removeIf(variableInstance -> variableInstance.getName().equals(TASK_VARIABLES_JSON_STRING));
        taskWithVariables.setVariableList(variableInstances);
        taskWithVariables.setIdentityLinkList(IDENTITY_LINK_MAPPER.identityLinksToIdentityLinksDto(taskService.getIdentityLinksForTask(updatedTask.getId())));

        return taskWithVariables;
    }

    public FilteredTasks queryAvailableTasksWithVariables(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        final int offset = jsonObject.getInt(OFFSET, 0);
        final int limit = jsonObject.getInt(LIMIT, 50);
        return taskFilterService.filterTasks(jsonObject, offset, limit);
    }

    public ActivitySummary queryActivitySummary(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        final int offset = jsonObject.getInt(OFFSET, 0);
        final int limit = jsonObject.getInt(LIMIT, 50);
        final ActivitySummaryProvider activitySummaryProvider = new ActivitySummaryProvider();
        final List<TaskWithVariables> taskWithVariables = taskFilterService.filterTasks(jsonObject, offset, limit).getTaskWithVariables();
        final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.Task> tasks = taskWithVariables.stream().map(TaskWithVariables::getTask).toList();
        return activitySummaryProvider.getActivitySummary(tasks);
    }

    public TaskWithVariables createGenericTaskAndVariables(final JsonEnvelope envelope) {

        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE));
        final CreateGenericTask createGenericTask = objectMapperProducer.objectMapper().convertValue(jsonObject, CreateGenericTask.class);

        final ProcessInstance genericTaskProcessInstance = runtimeService
                .createProcessInstanceByKey(genericTaskProcessKey)
                .setVariable(GENERIC_TASK_VARIABLES_JSON_STRING, createGenericTask)
                .setVariable(USER_ID, userId)
                .execute();
        LOGGER.info("generic task processInstance id : {}", genericTaskProcessInstance.getProcessInstanceId());
        final List<Task> tasks = taskService.createTaskQuery().processInstanceId(genericTaskProcessInstance.getProcessInstanceId()).list();

        final Task updatedTask = tasks.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Generic Task not created !!"));

        final TaskWithVariables taskWithVariables = new TaskWithVariables(TASK_MAPPER.taskToTaskDto(updatedTask));
        taskWithVariables.setVariableList(VARIABLE_INSTANCE_MAPPER.variableInstancesToVariableInstancesDto(runtimeService.createVariableInstanceQuery().taskIdIn(updatedTask.getId()).list()));
        taskWithVariables.setIdentityLinkList(IDENTITY_LINK_MAPPER.identityLinksToIdentityLinksDto(taskService.getIdentityLinksForTask(updatedTask.getId())));

        return taskWithVariables;
    }

    public TaskWithVariables updateTask(JsonEnvelope envelope) {
        final JsonObject inputJson = envelope.payloadAsJsonObject();
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE));
        final TaskQuery taskQuery = taskService.createTaskQuery();
        final String dueDateString = inputJson.getString(DUE_DATE, EMPTY);
        final String taskId = inputJson.getString(TASK_ID, "");
        final String note = inputJson.getString(NOTE, "");
        taskQuery.taskId(taskId);
        final Task camundaTask = taskQuery.orderByTaskName().asc().list().get(0);
        final String previousDueDateString = nonNull(camundaTask.getDueDate()) ? dateFormat.format(camundaTask.getDueDate()) : EMPTY;

        if (isNotEmpty(dueDateString)) {
            camundaTask.setDueDate(convertToDate(dueDateString));
        }

        taskService.saveTask(camundaTask);

        taskService.setVariableLocal(taskId, DUE, dueDateString);
        taskService.setVariableLocal(taskId, PREVIOUS_DUE, previousDueDateString);

        final String userDetails = userService.getUserDetails(userId);

        if (isNotEmpty(note)) {
            taskService.setVariable(taskId, NOTE, note);
            taskService.setVariableLocal(taskId, NOTE_LAST_UPDATED_DATE, ZonedDateTime.now().toString());
            taskService.setVariableLocal(taskId, NOTE_LAST_UPDATED_USER, userDetails);
        }

        taskService.setVariableLocal(taskId, LAST_UPDATED_BY_ID, userId);
        if (userDetails != null) {
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userDetails);
        } else {
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userId);
        }
        return new TaskWithVariables(TASK_MAPPER.taskToTaskDto(camundaTask));
    }

    public TaskWithVariables reopenTask(JsonEnvelope envelope) throws JsonProcessingException {
        final JsonObject inputJson = envelope.payloadAsJsonObject();
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE));
        final String userDetails = userService.getUserDetails(userId);
        final String oldTaskId = inputJson.getString(TASK_ID);

        final String reopenReason = inputJson.getString(REASON);
        final String newCandidateGroups = inputJson.getString(CANDIDATE_GROUPS);
        final String newAssignee = inputJson.getString(ASSIGNEE);
        final String newDueDate = inputJson.getString(DUE_DATE);

        // Retrieve historic variable instances associated with the task
        final HistoricTaskInstance oldTaskInstance = historyService.createHistoricTaskInstanceQuery()
                .taskId(oldTaskId).singleResult();

        List<HistoricVariableInstance> oldTaskVariables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(oldTaskInstance.getProcessInstanceId()).list();

        oldTaskVariables.stream().forEach(tv -> {
            LOGGER.error("oldTaskVariable name: {}, value:{}", tv.getName(), nonNull(tv.getValue()) ? tv.getValue().toString() : "NULL");
        });

        final HistoricVariableInstance oldTaskTypeId = oldTaskVariables.stream()
                .filter(variable -> TASK_TYPE_ID.equals(variable.getName())).findFirst().orElseThrow(() -> new IllegalArgumentException(TASK_TYPE_ID_NOT_FOUND));

        final WorkflowTaskType workflowTaskType = referenceDataService.getWorkFlowTaskTypeWithId((UUID) oldTaskTypeId.getValue());

        // Set variables for the new task based on historic variables
        final Map<String, Object> newVariablesMap = new HashMap<>();
        WorkflowCustomTaskType newCustomTaskType = new WorkflowCustomTaskType();

        oldTaskVariables.forEach(variable ->
                setWorkflowCustomTaskTypeFields(newCustomTaskType, newVariablesMap, variable));

        newCustomTaskType.setTaskName(workflowTaskType.getTaskName());
        newCustomTaskType.setTaskType("activity");
        newCustomTaskType.setDue(newDueDate);
        newCustomTaskType.setDisplayName(workflowTaskType.getDisplayName());
        newCustomTaskType.setAssignee(newAssignee);

        newVariablesMap.put(TASK_VARIABLES_JSON_STRING, objectMapperProducer.objectMapper().writeValueAsString(newCustomTaskType));
        newVariablesMap.put(USER_ID, userId);
        newVariablesMap.put(CUSTOM_TASK_TYPE, newCustomTaskType.getTaskName());
        newVariablesMap.put(TASK_TYPE, newCustomTaskType.getTaskType());
        newVariablesMap.put(LAST_UPDATED_BY_ID, userId);
        newVariablesMap.put(LAST_UPDATED_BY_NAME, userDetails);
        newVariablesMap.put(PREVIOUS_DUE, newDueDate);

        for (Map.Entry<String, Object> entry : newVariablesMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            LOGGER.error("newVariable name: {}, value:{}", key, nonNull(value) ? value.toString() : "NULL");
        }

        final ProcessInstance customTaskProcessInstance = runtimeService
                .createProcessInstanceByKey(getTaskDefinitionKey(newCustomTaskType.getTaskName()))
                .setVariables(newVariablesMap)
                .execute();

        final Task newTask = taskService.createTaskQuery().processInstanceId(customTaskProcessInstance.getProcessInstanceId()).singleResult();
        final String newTaskId = newTask.getId();

        // Add comments to new and old activities
        List<Comment> comments = taskService.getTaskComments(oldTaskId);
        comments.forEach(comment ->
                taskService.createComment(newTaskId, customTaskProcessInstance.getProcessInstanceId(), comment.getFullMessage()));

        taskService.createComment(newTaskId, customTaskProcessInstance.getProcessInstanceId(),
                String.format("Reopened - %s. Old activity id is %s.", reopenReason, oldTaskId));

        taskService.createComment(oldTaskId, oldTaskInstance.getProcessInstanceId(),
                String.format("Reopened - %s. New activity id is %s.", reopenReason, newTaskId));

        taskService.addCandidateGroup(newTaskId, newCandidateGroups);

        if (isNotEmpty(newAssignee)) {
            final String assigneeDetails = userService.getUserDetails(newAssignee);
            taskService.setVariableLocal(newTaskId, LAST_UPDATED_BY_ID, userId);
            taskService.setVariableLocal(newTaskId, LAST_UPDATED_BY_NAME, userDetails);
            taskService.setVariableLocal(newTaskId, ASSIGN_TO_NAME, assigneeDetails);
            taskService.setAssignee(newTaskId, userId);
        }

        final Task createdTask = taskService.createTaskQuery().taskId(newTaskId).singleResult();
        final TaskWithVariables taskWithVariables = new TaskWithVariables(TASK_MAPPER.taskToTaskDto(createdTask));
        taskWithVariables.setVariableList(VARIABLE_INSTANCE_MAPPER.variableInstancesToVariableInstancesDto(runtimeService.createVariableInstanceQuery().taskIdIn(createdTask.getId()).list()));
        taskWithVariables.setIdentityLinkList(IDENTITY_LINK_MAPPER.identityLinksToIdentityLinksDto(taskService.getIdentityLinksForTask(createdTask.getId())));

        return taskWithVariables;
    }

    private WorkflowCustomTaskType setWorkflowCustomTaskTypeFields(WorkflowCustomTaskType workflowCustomTaskType, final Map<String, Object> newVariablesMap, final HistoricVariableInstance variable) {
        if (isNotEmpty(variable.getName())) {
            final String name = variable.getName();
            final Object value = variable.getValue();
            if (HEARING_DATE.equals(name) && nonNull(value)) {
                workflowCustomTaskType.setHearingDate(convertToLocalDate(value));
            } else if (NOTE.equals(name) && nonNull(value)) {
                workflowCustomTaskType.setNote((String) value);
            } else if (IS_URGENT.equals(name)) {
                workflowCustomTaskType.setUrgent((Boolean) value);
            } else if (IS_DEFERRABLE.equals(name)) {
                workflowCustomTaskType.setDeferrable((Boolean) value);
            } else if (IS_DELETABLE.equals(name)) {
                workflowCustomTaskType.setDeletable((Boolean) value);
            } else if (CASE_URN.equals(name)) {
                workflowCustomTaskType.setCaseURN((String) value);
            } else if (CASE_ID.equals(name)) {
                workflowCustomTaskType.setCaseId((String) value);
            } else if (DEFENDANTS.equals(name) && nonNull(value) && isNotEmpty(value.toString())) {
                ObjectMapper objectMapper = objectMapperProducer.objectMapper();
                try {
                    workflowCustomTaskType.setDefendants(objectMapperProducer.objectMapper().readValue((String) value, objectMapper.getTypeFactory().constructCollectionType(List.class, Defendant.class)));
                } catch (JsonProcessingException e) {
                    LOGGER.error("Error while parsing defendants list", e);
                }
            } else if (COURT_CODES.equals(name)) {
                workflowCustomTaskType.setCourtCodes((String) value);
            } else if (COMMENT.equals(name)) {
                workflowCustomTaskType.setComment((String) value);
            } else if (nonNull(value) && !TASK_VARIABLES_JSON_STRING.equals(name)
                    && !USER_ID.equals(name) && !ASSIGNEE.equals(name) && !ASSIGN_TO_NAME.equals(name)
                    && !LAST_UPDATED_BY_ID.equals(name) && !LAST_UPDATED_BY_NAME.equals(name)
                    && !DELETION_REASON.equals(name) && !CANCELLATION_REASON.equals(name) && !COMPLETION_REASON.equals(name)
                    && !TASK_NAME.equals(name) && !TASK_DISPLAY_NAME.equals(name)
                    && !TASK_TYPE_ID.equals(name) && !TASK_TYPE.equals(name) && !CUSTOM_TASK_TYPE.equals(name)
                    && !DUE.equals(name) && !DUE_DATE.equals(name) && !PREVIOUS_DUE.equals(name) && !PREVIOUS_WORK_QUEUE.equals(name)
            ) {
                newVariablesMap.put(name, value);
            }
        }
        return workflowCustomTaskType;
    }

    public TaskWithVariables updateTaskVariableLocal(JsonEnvelope envelope) {
        final JsonObject inputJson = envelope.payloadAsJsonObject();
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE));
        final TaskQuery taskQuery = taskService.createTaskQuery();
        final String taskId = inputJson.getString(TASK_ID, EMPTY);
        taskQuery.taskId(taskId);
        final Task camundaTask = getFirstTask(taskQuery);

        final String varName = inputJson.getString(VAR_NAME, EMPTY);

        if (WORK_QUEUE.equals(varName)) {
            final String previousWorkQueue = nonNull(taskService.getVariableLocal(taskId, WORK_QUEUE)) ?
                    taskService.getVariableLocal(taskId, WORK_QUEUE).toString() : EMPTY;
            taskService.setVariableLocal(taskId, PREVIOUS_WORK_QUEUE, previousWorkQueue);
        }

        if (IS_URGENT.equals(varName)) {
            final Boolean varValue = inputJson.getBoolean("value");
            taskService.setVariableLocal(taskId, varName, varValue);
        } else {
            final String varValue = inputJson.getString("value", EMPTY);
            taskService.setVariableLocal(taskId, varName, varValue);
        }

        final String userDetails = userService.getUserDetails(userId);

        taskService.setVariableLocal(taskId, LAST_UPDATED_BY_ID, userId);
        if (userDetails != null) {
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userDetails);
        } else {
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userId);
        }
        return new TaskWithVariables(TASK_MAPPER.taskToTaskDto(camundaTask));
    }

    private static Task getFirstTask(final TaskQuery taskQuery) {
        final List<Task> tasks = taskQuery
                .orderByTaskName()
                .asc() // Ensure you have the direction set
                .list();

        if (tasks.isEmpty()) {
            throw new InputDataException("Something went wrong");
        }
        return tasks.get(0);
    }

    public TaskWithVariables getTaskDetails(final JsonEnvelope envelope) {
        final JsonObject inputJson = envelope.payloadAsJsonObject();
        final String taskId = inputJson.getString(TASK_ID, "");

        if (taskId.isEmpty()) {
            throw new BadRequestException(TASK_ID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE);
        }

        final HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .singleResult();
        if (isNull(historicTaskInstance)) {
            throw new BadRequestException(TASK_NOT_FOUND_BY_TASK_ID);
        }

        uk.gov.moj.cpp.workmanagement.proxy.api.model.Task task = TASK_MAPPER.historicTaskInstanceToTaskDto(historicTaskInstance);
        TaskWithVariables taskWithVariables = new TaskWithVariables(task);

        List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery().processInstanceId(historicTaskInstance.getProcessInstanceId()).list();

        Map<String, Object> historicVariablesMap = new HashMap<>();
        for (HistoricVariableInstance historicVariableInstance : historicVariables) {
            historicVariablesMap.put(historicVariableInstance.getName(), historicVariableInstance.getValue());
        }
        taskWithVariables.setVariableMap(historicVariablesMap);
        final List<Comment> taskComments = taskService.getTaskComments(task.getId());
        taskWithVariables.setCommentCount((isNull(taskComments) || taskComments.isEmpty()) ? 0 : taskComments.size());
        taskWithVariables.setIdentityLinkList(IDENTITY_LINK_MAPPER.historicIdentityLinksLogToIdentityLinksDto(historyService.createHistoricIdentityLinkLogQuery().taskId(historicTaskInstance.getId()).list()));

        return taskWithVariables;
    }

    private String getTaskDefinitionKey(final String taskName){
        return taskName + CUSTOM_PROCESS_SUFFIX;
    }
}