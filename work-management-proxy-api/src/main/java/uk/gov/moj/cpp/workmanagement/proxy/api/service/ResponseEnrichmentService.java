package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.time.ZonedDateTime.now;

import uk.gov.moj.cpp.workmanagement.proxy.api.exception.InputDataException;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.TaskResponse;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseEnrichmentService {

    private static final String TASK_VARIABLES_JSON_STRING = "taskVariablesJsonString";
    private static final String CANDIDATE_GROUPS = "candidateGroups";
    private static final String TASK_OVERDUE_FLAG = "taskOverdueFlag";

    public List<TaskResponse> buildAndEnrichTaskResponseList(final List<TaskWithVariables> taskList) {
        final List<TaskResponse> enrichedTaskList = new ArrayList<>();
        taskList.forEach(task -> {
            final HashMap<String, Object> variables = new HashMap<>();
            task.getVariableList().forEach(variable ->
                    manageVariable(variable, variables)
            );
            final TaskResponse taskResponse = TaskResponse.convertToResponse(task);
            taskResponse.setVariables(variables);
            enrichedTaskList.add(taskResponse);
        });
        return enrichedTaskList;
    }

    public TaskResponse buildAndEnrichTaskResponseList(final TaskWithVariables task) {
        final HashMap<String, Object> variables = new HashMap<>();
        task.getVariableList().forEach(variable ->
                variables.put(variable.getName(), variable.getValue())
        );
        final TaskResponse taskResponse = TaskResponse.convertToResponse(task);
        taskResponse.setVariables(variables);

        return taskResponse;
    }

    public TaskResponse enrichWithVariablesListForGetTaskDetails(final TaskWithVariables task) {
        final Map<String, Object> variables = task.getVariableMap();

        setVariables(variables);
        setTaskOverFlowFlag(variables, task.getTask().getDueDate());

        final TaskResponse taskResponse = TaskResponse.convertToResponse(task);
        taskResponse.setVariables(variables);

        return taskResponse;
    }

    private void setVariables(final Map<String, Object> variablesMap) {
        if (variablesMap != null && variablesMap.containsKey(TASK_VARIABLES_JSON_STRING)) {
            final ObjectMapper objectMapper = new ObjectMapper();
            try {
                final Map taskDetailsMap = objectMapper.readValue((String) variablesMap.get(TASK_VARIABLES_JSON_STRING), Map.class);
                if (taskDetailsMap != null) {
                    taskDetailsMap.forEach((key, value) -> setVariableValues(variablesMap, key, value));
                }
            } catch (IOException exception) {
                throw new InputDataException("Unable to convert " + exception);
            }
            variablesMap.remove(TASK_VARIABLES_JSON_STRING);
        }
    }

    private void setVariableValues(final Map<String, Object> variablesMap, Object key, Object value) {
        if (variablesMap != null && (!variablesMap.containsKey(key) && !CANDIDATE_GROUPS.equals(key))) {
            variablesMap.put((String) key, value);
        }
    }

    private void setTaskOverFlowFlag(final Map<String, Object> variablesMap, Date dueDate) {

        if (dueDate != null && variablesMap != null) {
            variablesMap.put(TASK_OVERDUE_FLAG, dueDate.before(Date.from(now(ZoneId.systemDefault()).toInstant())));
        }
    }


    private void manageVariable(final VariableInstance variable, final HashMap<String, Object> variables) {
        variables.put(variable.getName(), variable.getValue());
    }
}
