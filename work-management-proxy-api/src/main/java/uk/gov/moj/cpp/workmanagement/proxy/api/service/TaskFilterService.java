package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.moj.cpp.workmanagement.proxy.api.mapper.TaskMapper.TASK_MAPPER;
import static uk.gov.moj.cpp.workmanagement.proxy.api.mapper.VariableInstanceMapper.VARIABLE_INSTANCE_MAPPER;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ACTIVITY_TYPES;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ASSIGNEE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.BUSINESS_UNIT_CODES;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANDIDATE_GROUPS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_TAG;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CASE_URN;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COMPLETED;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COURT_CODES;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CUSTODY_TIME_LIMIT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DELETED;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DUE_DAYS_WITH_IN;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.FOLLOW_UP;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.FOLLOW_UP_DAYS_WITH_IN;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.HEARING_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.HEARING_DAYS_WITH_IN;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_ASSIGNED;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_CTL_ONLY;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_DUE_SOON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_OVERDUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.IS_URGENT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.REGION;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_STATUS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.WORK_QUEUE_TYPE;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.FilteredTasks;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstanceQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@SuppressWarnings({"squid:S134", "squid:S3776"})
public class TaskFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskFilterService.class);
    private static final Integer DUE_SOON_CHECK_DAYS = 2;

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private TaskService taskService;

    @Inject
    private HistoryService historyService;

    public FilteredTasks filterTasks(JsonObject filterInputJsonObject) {
        return filterTasks(filterInputJsonObject, null, null);
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    public FilteredTasks filterTasks(JsonObject filterInputJsonObject, Integer offset, Integer limit) {
        final UUID tid = randomUUID();
        LOGGER.info("tid : {} -- filters applied on tasks : {}", tid, filterInputJsonObject);

        final String taskStatus = String.valueOf(filterInputJsonObject.getString(TASK_STATUS, EMPTY));
        if (COMPLETED.equalsIgnoreCase(taskStatus) || DELETED.equalsIgnoreCase(taskStatus)) {
            // Use HistoryService for completed or deleted tasks
            return filterHistoricTasks(filterInputJsonObject, offset, limit, tid);
        } else {
            // Use TaskService for active tasks
            return filterActiveTasks(filterInputJsonObject, offset, limit, tid);
        }
    }

    private FilteredTasks filterHistoricTasks(JsonObject filterInputJsonObject, Integer offset, Integer limit, UUID tid){
        final HistoricTaskInstanceQuery historicTaskQuery = historyService.createHistoricTaskInstanceQuery();

        addFilterForIsAssigned(filterInputJsonObject, historicTaskQuery, null);
        addFilterForAssignee(filterInputJsonObject, historicTaskQuery, null);
        addFilterForHearingDate(filterInputJsonObject, historicTaskQuery, null);
        addFilterForUserRoles(filterInputJsonObject, historicTaskQuery, null);
        addFilterForActivityStatus(filterInputJsonObject, historicTaskQuery);
        addFilterForActivityType(filterInputJsonObject, historicTaskQuery);
        addFilterForOverDue(filterInputJsonObject, historicTaskQuery, null);
        addFilterForDueSoon(filterInputJsonObject, historicTaskQuery);
        addFilterForDueDate(filterInputJsonObject, historicTaskQuery);
        addFilterForFollowupDate(filterInputJsonObject, historicTaskQuery, null);
        addFilterForLocalVariables(filterInputJsonObject, historicTaskQuery);

        final List<HistoricTaskInstance> filteredTasks = nonNull(offset) && nonNull(limit) ?
                historicTaskQuery.orderByTaskDueDate().asc().listPage(offset, limit) :
                historicTaskQuery.orderByTaskDueDate().asc().list();

        long taskCount = historicTaskQuery.count();

        if (filteredTasks.isEmpty()) {
            LOGGER.info("tid : {} -- No task returned from camunda query", tid);
            return FilteredTasks.builder().taskWithVariables(new ArrayList<>()).totalTaskCount(0).build();
        }
        LOGGER.info("tid : {} -- tasks.size() : {}", tid, filteredTasks.size());

        List<TaskWithVariables> taskWithVariables = getTaskWithVariablesFromHistoricTaskInstance(filteredTasks);
        return FilteredTasks.builder().taskWithVariables(taskWithVariables).totalTaskCount(taskCount).build();
    }

    private FilteredTasks filterActiveTasks(JsonObject filterInputJsonObject, Integer offset, Integer limit, UUID tid){
        // Use TaskService for active tasks
        final TaskQuery taskQuery = taskService.createTaskQuery();
        addFilterForIsAssigned(filterInputJsonObject, null, taskQuery);
        addFilterForAssignee(filterInputJsonObject, null, taskQuery);
        addFilterForHearingDate(filterInputJsonObject, null, taskQuery);
        addFilterForUserRoles(filterInputJsonObject, null, taskQuery);
        addFilterForActivityType(filterInputJsonObject, taskQuery);
        addFilterForOverDue(filterInputJsonObject, null, taskQuery);
        addFilterForDueSoon(filterInputJsonObject, taskQuery);
        addFilterForDueDate(filterInputJsonObject, taskQuery);
        addFilterForFollowupDate(filterInputJsonObject, null, taskQuery);
        addFilterForLocalVariables(filterInputJsonObject, taskQuery);

        final List<Task> filteredTasks = nonNull(offset) && nonNull(limit) ?
                taskQuery.orderByDueDate().asc().listPage(offset, limit) :
                taskQuery.orderByDueDate().asc().list();

        long taskCount = taskQuery.count();

        if (filteredTasks.isEmpty()) {
            LOGGER.info("tid : {} -- No active task returned from camunda query", tid);
            return FilteredTasks.builder().taskWithVariables(new ArrayList<>()).totalTaskCount(0).build();
        }
        LOGGER.info("tid : {} -- active tasks.size() : {}", tid, filteredTasks.size());
        List<TaskWithVariables> taskWithVariables = getTaskWithVariablesFromTaskInstance(filteredTasks);
        return FilteredTasks.builder().taskWithVariables(taskWithVariables).totalTaskCount(taskCount).build();
    }

    private void addFilterForActivityType(final JsonObject jsonObject, final HistoricTaskInstanceQuery taskQuery) {
        final String[] activityTypes = jsonObject.containsKey(ACTIVITY_TYPES)
                ? jsonObject.getString(ACTIVITY_TYPES).split(",")
                : null;

        if (nonNull(activityTypes) && activityTypes.length > 0) {
            taskQuery.taskDefinitionKeyIn(activityTypes);
        }
    }

    private void addFilterForActivityType(final JsonObject jsonObject, final TaskQuery taskQuery) {
        final String[] activityTypes = jsonObject.containsKey(ACTIVITY_TYPES)
                ? jsonObject.getString(ACTIVITY_TYPES).split(",")
                : null;

        if (nonNull(activityTypes) && activityTypes.length > 0) {
            taskQuery.taskDefinitionKeyIn(activityTypes);
        }
    }

    private void addFilterForOverDue(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskQuery, final TaskQuery taskQuery) {
        final Boolean isOverdue = jsonObject.containsKey(IS_OVERDUE) ? jsonObject.getBoolean(IS_OVERDUE) : null;
        if (nonNull(isOverdue) && isOverdue) {
            if (historicTaskQuery != null) {
                historicTaskQuery.taskDueBefore(new Date());
            } else if (taskQuery != null) {
                taskQuery.dueBefore(new Date());
            }
        }
    }

    private void addFilterForDueSoon(final JsonObject jsonObject, final HistoricTaskInstanceQuery taskQuery) {
        final Boolean isDueSoon = jsonObject.containsKey(IS_DUE_SOON) ? jsonObject.getBoolean(IS_DUE_SOON) : null;
        if (nonNull(isDueSoon) && isDueSoon) {
            final LocalDate today = LocalDate.now();
            final LocalDate dueSoon = today.plusDays((long)DUE_SOON_CHECK_DAYS + 1);
            taskQuery.taskDueBefore(java.sql.Date.valueOf(dueSoon));
            taskQuery.taskDueAfter(java.sql.Date.valueOf(today));
        }
    }

    private void addFilterForDueSoon(final JsonObject jsonObject, final TaskQuery taskQuery) {
        final Boolean isDueSoon = jsonObject.containsKey(IS_DUE_SOON) ? jsonObject.getBoolean(IS_DUE_SOON) : null;
        if (nonNull(isDueSoon) && isDueSoon) {
            final LocalDate today = LocalDate.now();
            final LocalDate dueSoon = today.plusDays((long)DUE_SOON_CHECK_DAYS + 1);
            taskQuery.dueBefore(java.sql.Date.valueOf(dueSoon));
            taskQuery.dueAfter(java.sql.Date.valueOf(today));
        }
    }

    private void addFilterForIsAssigned(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskQuery, final TaskQuery taskQuery) {
        final Boolean isAssigned = jsonObject.containsKey(IS_ASSIGNED) ? jsonObject.getBoolean(IS_ASSIGNED) : null;
        if (nonNull(isAssigned)) {
            if (isAssigned) {
                if (historicTaskQuery != null) {
                    historicTaskQuery.taskAssigned();
                } else if (taskQuery != null) {
                    taskQuery.taskAssigned();
                }
            } else {
                if (historicTaskQuery != null) {
                    historicTaskQuery.taskUnassigned();
                } else if (taskQuery != null) {
                    taskQuery.taskUnassigned();
                }
            }
        }
    }

    private void addFilterForUserRoles(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskQuery, final TaskQuery taskQuery) {
        final List<String> userRoleOrGroupList = jsonObject.containsKey(CANDIDATE_GROUPS)
                ? asList(jsonObject.getString(CANDIDATE_GROUPS).split(","))
                : null;
        if (nonNull(userRoleOrGroupList)) {
            if (historicTaskQuery != null) {
                userRoleOrGroupList.forEach(historicTaskQuery::taskHadCandidateGroup);
            } else if (taskQuery != null) {
                userRoleOrGroupList.forEach(taskQuery::taskCandidateGroup);
            }
        }
    }

    private void addFilterForActivityStatus(final JsonObject jsonObject, final HistoricTaskInstanceQuery taskQuery) {
        final String taskStatus = String.valueOf(jsonObject.getString(TASK_STATUS, EMPTY));
        if (COMPLETED.equalsIgnoreCase(taskStatus)) {
            taskQuery.taskDeleteReason(COMPLETED);
        } else if (DELETED.equalsIgnoreCase(taskStatus)) {
            taskQuery.taskDeleteReason(DELETED);
        } else {
            taskQuery.unfinished();
        }
    }

    private void addFilterForDueDate(final JsonObject jsonObject, final HistoricTaskInstanceQuery taskQuery) {
        if (jsonObject.containsKey(DUE)) {
            final int dueDaysWithIn = jsonObject.getInt(DUE_DAYS_WITH_IN, 0);
            final LocalDate filterDueDate = parse(jsonObject.getString(DUE));

            final LocalDate before = filterDueDate.minusDays(Integer.toUnsignedLong(dueDaysWithIn));
            final LocalDate after = filterDueDate.plusDays(Integer.toUnsignedLong(dueDaysWithIn + 1));

            Date beforeDate = Date.from(before.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date afterDate = Date.from(after.atStartOfDay(ZoneId.systemDefault()).toInstant());

            taskQuery.taskDueAfter(beforeDate).taskDueBefore(afterDate);
        }
    }

    private void addFilterForDueDate(final JsonObject jsonObject, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(DUE)) {
            final int dueDaysWithIn = jsonObject.getInt(DUE_DAYS_WITH_IN, 0);
            final LocalDate filterDueDate = parse(jsonObject.getString(DUE));

            final LocalDate before = filterDueDate.minusDays(Integer.toUnsignedLong(dueDaysWithIn));
            final LocalDate after = filterDueDate.plusDays(Integer.toUnsignedLong(dueDaysWithIn + 1));

            Date beforeDate = Date.from(before.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date afterDate = Date.from(after.atStartOfDay(ZoneId.systemDefault()).toInstant());

            taskQuery.dueAfter(beforeDate).dueBefore(afterDate);
        }
    }

    private void addFilterForFollowupDate(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(FOLLOW_UP)) {
            final int followupDaysWithIn = jsonObject.getInt(FOLLOW_UP_DAYS_WITH_IN, 0);
            final LocalDate filterFollowupDate = parse(jsonObject.getString(FOLLOW_UP));

            final LocalDate after = filterFollowupDate.plusDays(Integer.toUnsignedLong(followupDaysWithIn + 1));
            final LocalDate before = filterFollowupDate.minusDays(Integer.toUnsignedLong(followupDaysWithIn + 1));

            Date beforeDate = Date.from(before.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date afterDate = Date.from(after.atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (followupDaysWithIn == 0) {
                if (historicTaskQuery != null) {
                    historicTaskQuery.processVariableValueEquals(FOLLOW_UP, filterFollowupDate);
                } else if (taskQuery != null) {
                    taskQuery.processVariableValueEquals(FOLLOW_UP, filterFollowupDate);
                }
            } else {
                if (historicTaskQuery != null) {
                    historicTaskQuery.taskDueAfter(beforeDate).taskDueBefore(afterDate);
                } else if (taskQuery != null) {
                    taskQuery.dueAfter(beforeDate).dueBefore(afterDate);
                }
            }
        }
    }

    private void addFilterForLocalVariables(final JsonObject jsonObject, final HistoricTaskInstanceQuery taskQuery) {
        addFilterForCaseURNLocalVariable(jsonObject, taskQuery, null);
        addFilterForCourtCodeLocalVariable(jsonObject, taskQuery, null);
        addFilterForBusinessUnitCodeLocalVariable(jsonObject, taskQuery, null);
        addFilterForRegionLocalVariable(jsonObject, taskQuery, null);
        addFilterForCtlLocalVariable(jsonObject, taskQuery, null);
        addFilterForIsUrgentLocalVariable(jsonObject, taskQuery, null);
        addFilterForWorkQueueLocalVariable(jsonObject, taskQuery, null);
        addFilterForCaseTagLocalVariable(jsonObject, taskQuery, null);
    }

    private void addFilterForLocalVariables(final JsonObject jsonObject, final TaskQuery taskQuery) {
        addFilterForCaseURNLocalVariable(jsonObject, null, taskQuery);
        addFilterForCourtCodeLocalVariable(jsonObject, null, taskQuery);
        addFilterForBusinessUnitCodeLocalVariable(jsonObject, null, taskQuery);
        addFilterForRegionLocalVariable(jsonObject, null, taskQuery);
        addFilterForCtlLocalVariable(jsonObject, null, taskQuery);
        addFilterForIsUrgentLocalVariable(jsonObject, null, taskQuery);
        addFilterForWorkQueueLocalVariable(jsonObject, null, taskQuery);
        addFilterForCaseTagLocalVariable(jsonObject, null, taskQuery);
    }

    private void addFilterForCaseURNLocalVariable(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(CASE_URN)) {
            String caseUrn = jsonObject.getString(CASE_URN);
            if (taskQuery != null) {
                taskQuery.or().taskVariableValueEquals(CASE_URN, caseUrn).processVariableValueEquals(CASE_URN, caseUrn).endOr();
            }else {
                historicTaskInstanceQuery.or().taskVariableValueEquals(CASE_URN, caseUrn).processVariableValueEquals(CASE_URN, caseUrn).endOr();
            }
        }
    }

    private void addFilterForCourtCodeLocalVariable(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(COURT_CODES)) {
            final String courtCodeString = jsonObject.getString(COURT_CODES);
            final List<String> courtCodeList = Arrays.asList(courtCodeString.split(","));
            if (nonNull(courtCodeList) && !courtCodeList.isEmpty()) {
                if (taskQuery != null) {
                    TaskQuery orQuery = taskQuery.or();
                    courtCodeList.forEach(courtCode -> orQuery.taskVariableValueEquals(COURT_CODES, courtCode));
                    orQuery.endOr();
                } else {
                    HistoricTaskInstanceQuery orQuery = historicTaskInstanceQuery.or();
                    courtCodeList.forEach(courtCode -> orQuery.taskVariableValueEquals(COURT_CODES, courtCode));
                    orQuery.endOr();
                }
            }
        }
    }

    private void addFilterForBusinessUnitCodeLocalVariable(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(BUSINESS_UNIT_CODES)) {
            final String businessUnitCodesString = jsonObject.getString(BUSINESS_UNIT_CODES);
            final List<String> businessUnitCodeList = Arrays.asList(businessUnitCodesString.split(","));
            if (taskQuery != null) {
                businessUnitCodeList.forEach(businessUnitCode -> taskQuery.taskVariableValueEquals(BUSINESS_UNIT_CODES, businessUnitCode));
            } else {
                businessUnitCodeList.forEach(businessUnitCode -> historicTaskInstanceQuery.taskVariableValueEquals(BUSINESS_UNIT_CODES, businessUnitCode));
            }
        }
    }

    private void addFilterForRegionLocalVariable(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(REGION)) {
            final String region = jsonObject.getString(REGION);
            if (taskQuery != null) {
                taskQuery.taskVariableValueEquals(REGION, region);
            } else {
                historicTaskInstanceQuery.taskVariableValueEquals(REGION, region);
            }
        }
    }

    private void addFilterForHearingDate(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(HEARING_DATE)) {
            final int hearingDaysWithIn = jsonObject.getInt(HEARING_DAYS_WITH_IN, 0);
            final LocalDate filterHearingDate = parse(jsonObject.getString(HEARING_DATE));

            final List<LocalDate> dateRange = new ArrayList<>();
            for (int i = -hearingDaysWithIn; i <= hearingDaysWithIn; i++) {
                dateRange.add(filterHearingDate.plusDays(i));
            }

            if (taskQuery != null) {
                TaskQuery orQuery = taskQuery.or();
                for (LocalDate date : dateRange) {
                    orQuery = orQuery.processVariableValueLike(HEARING_DATE, date.toString() + "%");
                }
                orQuery.endOr();
            } else {
                HistoricTaskInstanceQuery orQuery = historicTaskInstanceQuery.or();
                for (LocalDate date : dateRange) {
                    orQuery = orQuery.processVariableValueLike(HEARING_DATE, date.toString() + "%");
                }
                orQuery.endOr();
            }
        }
    }

    private void addFilterForCtlLocalVariable(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(IS_CTL_ONLY)) {
            if (taskQuery != null) {
                taskQuery.processVariableValueNotEquals(CUSTODY_TIME_LIMIT, null);
            } else {
                historicTaskInstanceQuery.processVariableValueNotEquals(CUSTODY_TIME_LIMIT, null);
            }
        }
    }

    private void addFilterForIsUrgentLocalVariable(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(IS_URGENT)) {
            final boolean isUrgent = jsonObject.getBoolean(IS_URGENT);
            if (taskQuery != null) {
                taskQuery.taskVariableValueEquals(IS_URGENT, isUrgent);
            } else {
                historicTaskInstanceQuery.taskVariableValueEquals(IS_URGENT, isUrgent);
            }
        }
    }

    private void addFilterForWorkQueueLocalVariable(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(WORK_QUEUE_TYPE)) {
            final String workQueueString = jsonObject.getString(WORK_QUEUE_TYPE);
            final List<String> workQueueList = Arrays.asList(workQueueString.split(","));
            if (taskQuery != null) {
                TaskQuery orQuery = taskQuery.or();
                workQueueList.forEach(workQueue -> orQuery.taskVariableValueEquals(WORK_QUEUE, workQueue));
                orQuery.endOr();
            } else {
                HistoricTaskInstanceQuery orQuery = historicTaskInstanceQuery.or();
                workQueueList.forEach(workQueue -> orQuery.taskVariableValueEquals(WORK_QUEUE, workQueue));
                orQuery.endOr();
            }
        }
    }

    private void addFilterForCaseTagLocalVariable(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskInstanceQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(CASE_TAG)) {
            final String caseTag = jsonObject.getString(CASE_TAG);
            if (taskQuery != null) {
                taskQuery.taskVariableValueEquals(CASE_TAG, caseTag);
            } else {
                historicTaskInstanceQuery.taskVariableValueEquals(CASE_TAG, caseTag);
            }
        }
    }

    private void addFilterForAssignee(final JsonObject jsonObject, final HistoricTaskInstanceQuery historicTaskQuery, final TaskQuery taskQuery) {
        if (jsonObject.containsKey(ASSIGNEE)) {
            String assignee = jsonObject.getString(ASSIGNEE);
            if (isNotEmpty(assignee)) {
                if (historicTaskQuery != null) {
                    historicTaskQuery.taskAssignee(assignee);
                } else if (taskQuery != null) {
                    taskQuery.taskAssignee(assignee);
                }
            } else {
                if (historicTaskQuery != null) {
                    historicTaskQuery.taskAssignee(null);
                } else if (taskQuery != null) {
                    taskQuery.taskAssignee(null);
                }
            }
        }
    }

    private List<TaskWithVariables> getTaskWithVariablesFromHistoricTaskInstance(final List<HistoricTaskInstance> filteredTasks) {
        final List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .executionIdIn(filteredTasks.stream()
                        .map(HistoricTaskInstance::getExecutionId)
                        .distinct()
                        .toArray(String[]::new))
                .disableBinaryFetching()
                .list();

        return filteredTasks.stream()
                .map(camundaTask -> {
                    uk.gov.moj.cpp.workmanagement.proxy.api.model.Task task = TASK_MAPPER.historicTaskInstanceToTaskDto(camundaTask);
                    TaskWithVariables ts = new TaskWithVariables(task);

                    Map<String, HistoricVariableInstance> distinctByName = variables.stream()
                            .filter(variable -> variable.getProcessInstanceId().equals(camundaTask.getProcessInstanceId()))
                            .collect(Collectors.toMap(
                                    HistoricVariableInstance::getName,
                                    v -> v,
                                    (existing, replacement) -> existing,
                                    LinkedHashMap::new
                            ));

                    ts.getVariableList().addAll(
                            VARIABLE_INSTANCE_MAPPER.historicVariableInstancesToVariableInstancesDto(
                                    new ArrayList<>(distinctByName.values())
                            )
                    );
                    return ts;
                }).toList();
    }

    private List<TaskWithVariables> getTaskWithVariablesFromTaskInstance(final List<Task> filteredTasks) {
        final List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
                .executionIdIn(filteredTasks.stream().map(Task::getExecutionId).distinct().toArray(String[]::new))
                .disableBinaryFetching()
                .list();

        Map<String, List<VariableInstance>> varsByProcInsId =
                variables.stream().collect(Collectors.groupingBy(VariableInstance::getProcessInstanceId));

        List<TaskWithVariables> result = new ArrayList<>(filteredTasks.size());
        for (Task camundaTask : filteredTasks) {
            var taskDto = TASK_MAPPER.taskToTaskDto(camundaTask);
            var taskWithVariables = new TaskWithVariables(taskDto);

            var vars = varsByProcInsId.getOrDefault(taskDto.getProcessInstanceId(), List.of());
            taskWithVariables.getVariableList().addAll(VARIABLE_INSTANCE_MAPPER.variableInstancesToVariableInstancesDto(vars));
            result.add(taskWithVariables);
        }
        return result;
    }
}
