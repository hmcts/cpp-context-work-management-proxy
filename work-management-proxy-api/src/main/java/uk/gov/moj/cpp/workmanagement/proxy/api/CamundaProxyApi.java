package uk.gov.moj.cpp.workmanagement.proxy.api;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ASSIGNEE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANDIDATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANDIDATE_GROUPS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COMMENT_COUNT;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CREATED;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DEFENDANTS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DUE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DUE_DATE;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.FIRST_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LAST_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.STATUS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASKS;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TOTAL_COUNT;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.CustomServiceComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.exception.InputDataException;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.ActivitySummary;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.FilteredTasks;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.IdentityLink;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.Task;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.TaskResponse;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.CamundaJavaApiService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.ResponseEnrichmentService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;
import uk.gov.moj.cps.workmanagement.proxy.api.Defendant;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CustomServiceComponent("WorkManagement.Proxy.API")
@SuppressWarnings({"squid:S134", "squid:S3776"})
public class CamundaProxyApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaProxyApi.class);
    @Inject
    private CamundaJavaApiService camundaJavaApiService;

    @Inject
    private ResponseEnrichmentService responseEnrichmentService;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final String exceptionThrown = "Exception Thrown ";
    private final String error = "error:";

    @Handles("custom-action")
    public JsonEnvelope nonProxyRestAction(final JsonEnvelope envelope) {
        return null;
    }

    @Handles("custom-query-assignee-tasks-with-variables")
    public JsonEnvelope getCustomQueryAssigneeTasksWithVariables(final JsonEnvelope envelope) {
        final FilteredTasks filteredTasks = camundaJavaApiService.queryTasksAssigneeWithVariables(envelope);

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();

        final List<TaskResponse> enrichedFilteredTasks = responseEnrichmentService.buildAndEnrichTaskResponseList(filteredTasks.getTaskWithVariables());
        enrichedFilteredTasks.forEach(j -> arrayBuilder.add(buildTaskJsonFromResponse(j)));
        return envelopeFrom(envelope.metadata(),
                createObjectBuilder()
                        .add(TOTAL_COUNT, filteredTasks.getTotalTaskCount())
                        .add(TASKS, arrayBuilder.build()).build());
    }


    @Handles("custom-query-available-tasks-with-variables")
    public JsonEnvelope getCustomQueryAvailableTasksWithVariables(final JsonEnvelope envelope) {
        final FilteredTasks filteredTasks = camundaJavaApiService.queryAvailableTasksWithVariables(envelope);

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final List<TaskResponse> enrichedFilteredTasks = responseEnrichmentService.buildAndEnrichTaskResponseList(filteredTasks.getTaskWithVariables());
        enrichedFilteredTasks.forEach(j -> arrayBuilder.add(buildTaskJsonFromResponse(j)));
        return envelopeFrom(envelope.metadata(),
                createObjectBuilder()
                        .add(TOTAL_COUNT, filteredTasks.getTotalTaskCount())
                        .add(TASKS, arrayBuilder.build()).build());
    }

    @Handles("get-task-details")
    public JsonEnvelope getTaskDetails(final JsonEnvelope envelope) {
        final TaskWithVariables task = camundaJavaApiService.getTaskDetails(envelope);
        final TaskResponse enrichedFilteredTask = responseEnrichmentService.enrichWithVariablesListForGetTaskDetails(task);
        return envelopeFrom(envelope.metadata(),
                createObjectBuilder()
                        .add(TASK, buildTaskJsonFromResponse(enrichedFilteredTask)).build());
    }

    @Handles("create-custom-task")
    public JsonEnvelope createCustomTaskAndVariables(final JsonEnvelope envelope) {
        final TaskWithVariables task = camundaJavaApiService.createCustomTaskAndVariables(envelope);

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final TaskResponse enrichedFilteredTask = responseEnrichmentService.buildAndEnrichTaskResponseList(task);
        arrayBuilder.add(buildTaskJsonFromResponse(enrichedFilteredTask));
        return envelopeFrom(envelope.metadata(),
                createObjectBuilder().add(TASK,
                        buildTaskJson(task.getTask(), task.getVariableList(), task.getIdentityLinkList())).build());
    }

    @Handles("update-task")
    public JsonEnvelope updateTask(final JsonEnvelope envelope) {
        try {
            final TaskWithVariables task = camundaJavaApiService.updateTask(envelope);
            return envelopeFrom(envelope.metadata(),
                    createObjectBuilder().add(TASK,
                            buildTaskJson(task.getTask(), task.getVariableList(), task.getIdentityLinkList())).build());

        } catch (InputDataException ex) {
            LOGGER.error(exceptionThrown, ex);
            return envelopeFrom(envelope.metadata(),
                    createObjectBuilder().add(error, ex.getLocalizedMessage()).build());
        }
    }

    @Handles("reopen-task")
    public JsonEnvelope reopenTask(final JsonEnvelope envelope) throws JsonProcessingException, ParseException {
        final TaskWithVariables task = camundaJavaApiService.reopenTask(envelope);

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final TaskResponse enrichTask = responseEnrichmentService.buildAndEnrichTaskResponseList(task);
        arrayBuilder.add(buildTaskJsonFromResponse(enrichTask));
        return envelopeFrom(envelope.metadata(),
                createObjectBuilder().add(TASK,
                        buildTaskJson(task.getTask(), task.getVariableList(), task.getIdentityLinkList())).build());
    }

    @Handles("assign-task")
    public JsonEnvelope assignTask(final JsonEnvelope envelope) {
        try {
            final TaskWithVariables task = camundaJavaApiService.assignTask(envelope);
            return envelopeFrom(envelope.metadata(),
                    createObjectBuilder().add(TASK,
                            buildTaskJson(task.getTask(), task.getVariableList(), task.getIdentityLinkList())).build());

        } catch (InputDataException ex) {
            LOGGER.error("Exception Thrown ", ex);
            return envelopeFrom(envelope.metadata(),
                    createObjectBuilder().add("error:", ex.getLocalizedMessage()).build());
        }
    }

    @Handles("update-task-variable")
    public JsonEnvelope updateTaskVariable(final JsonEnvelope envelope) {
        try {
            final TaskWithVariables task = camundaJavaApiService.updateTaskVariableLocal(envelope);
            return envelopeFrom(envelope.metadata(),
                    createObjectBuilder().add(TASK,
                            buildTaskJson(task.getTask(), task.getVariableList(), task.getIdentityLinkList())).build());

        } catch (InputDataException ex) {
            LOGGER.error(exceptionThrown, ex);
            return envelopeFrom(envelope.metadata(),
                    createObjectBuilder().add(error, ex.getLocalizedMessage()).build());
        }
    }

    @Handles("activity-summary")
    public JsonEnvelope getActivitySummary(final JsonEnvelope envelope) {
        final ActivitySummary activitySummary = camundaJavaApiService.queryActivitySummary(envelope);

        activitySummary.setActivities(activitySummary.getActivities());

        final ObjectToJsonValueConverter objectConvertor = new ObjectToJsonValueConverter(objectMapper);

        return envelopeFrom(envelope.metadata(), objectConvertor.convert(activitySummary));
    }

    @Handles("create-generic-task")
    public JsonEnvelope createGenericTaskAndVariables(final JsonEnvelope envelope) {
        final TaskWithVariables task = camundaJavaApiService.createGenericTaskAndVariables(envelope);

        return envelopeFrom(envelope.metadata(),
                createObjectBuilder().add(TASK,
                        buildTaskJson(task.getTask(), task.getVariableList(), task.getIdentityLinkList())).build());
    }


    private JsonObject buildTaskJson(final Task task, final List<VariableInstance> instances, final List<IdentityLink> identityLinkList) {

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        addVariablesToJsonBuilder(instances, jsonObjectBuilder);

        jsonObjectBuilder.add(ID, task.getId());
        jsonObjectBuilder.add(NAME, task.getName());
        jsonObjectBuilder.add(DUE, nonNull(task.getDueDate()) ? dateFormat.format(task.getDueDate()) : "");
        jsonObjectBuilder.add(ASSIGNEE, nonNull(task.getAssignee()) ? task.getAssignee() : "");
        jsonObjectBuilder.add(CREATED, nonNull(task.getCreateTime()) ? dateFormat.format(task.getCreateTime()) : "");

        final ObjectToJsonValueConverter objectConvertor = new ObjectToJsonValueConverter(objectMapper);
        final List<String> candidateGroups = identityLinkList.stream()
                .filter(id -> CANDIDATE.equalsIgnoreCase(id.getType()))
                .map(IdentityLink::getGroupId)
                .collect(toList());
        jsonObjectBuilder.add(CANDIDATE_GROUPS, objectConvertor.convert(candidateGroups));

        return jsonObjectBuilder.build();
    }


    private JsonObject buildTaskJsonFromResponse(final TaskResponse taskResponse) {

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        addResponseVariables(taskResponse.getVariables(), jsonObjectBuilder);

        jsonObjectBuilder.add(ID, taskResponse.getId());
        jsonObjectBuilder.add(NAME, taskResponse.getName());
        jsonObjectBuilder.add(DUE, nonNull(taskResponse.getDueDate()) ? dateFormat.format(taskResponse.getDueDate()) : "");
        jsonObjectBuilder.add(ASSIGNEE, nonNull(taskResponse.getAssignee()) ? taskResponse.getAssignee() : "");
        jsonObjectBuilder.add(CREATED, nonNull(taskResponse.getCreateTime()) ? dateFormat.format(taskResponse.getCreateTime()) : "");

        final ObjectToJsonValueConverter objectConvertor = new ObjectToJsonValueConverter(objectMapper);
        final List<String> candidateGroups = taskResponse.getIdentityLinkList().stream()
                .filter(id -> CANDIDATE.equalsIgnoreCase(id.getType()))
                .map(IdentityLink::getGroupId)
                .collect(toList());
        jsonObjectBuilder.add(CANDIDATE_GROUPS, objectConvertor.convert(candidateGroups));
        jsonObjectBuilder.add(COMMENT_COUNT, objectConvertor.convert(taskResponse.getCommentCount()));
        jsonObjectBuilder.add(STATUS, nonNull(taskResponse.getStatus()) ? taskResponse.getStatus() : "");
        return jsonObjectBuilder.build();
    }

    protected void addResponseVariables(final Map<String, Object> map, final JsonObjectBuilder jsonObjectBuilder) {
        map.forEach((key, value) -> {
            if (nonNull(value)) {
                addResponseVariablesToJsonBuilder(key, value, jsonObjectBuilder);
            }
        });
    }

    protected void addResponseVariablesToJsonBuilder(final String variableName, final Object variableValue, final JsonObjectBuilder jsonObjectBuilder) {
        if (!DUE.equals(variableName) && !DUE_DATE.equals(variableName)) {
            if (variableValue instanceof Boolean) {
                jsonObjectBuilder.add(variableName, (boolean) variableValue);
            } else if (variableValue instanceof Integer) {
                jsonObjectBuilder.add(variableName, (int) variableValue);
            } else if (variableValue instanceof Long) {
                jsonObjectBuilder.add(variableName, (long) variableValue);
            } else if (variableValue instanceof Double) {
                jsonObjectBuilder.add(variableName, (double) variableValue);
            } else if (variableValue instanceof BigDecimal) {
                jsonObjectBuilder.add(variableName, (BigDecimal) variableValue);
            } else if (variableValue instanceof BigInteger) {
                jsonObjectBuilder.add(variableName, (BigInteger) variableValue);
            } else if (DEFENDANTS.equalsIgnoreCase(variableName)) {
                jsonObjectBuilder.add(variableName, getJsonArrayBuilder(variableValue.toString()));
            } else {
                jsonObjectBuilder.add(variableName, variableValue.toString());
            }
        }
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    protected void addVariablesToJsonBuilder(final List<VariableInstance> instances, final JsonObjectBuilder jsonObjectBuilder) {
        for (final VariableInstance inst : instances) {
            if (isNull(inst)) {
                continue;
            }
            final Object value = getValueOfVarInstanceVariable(inst);
            if (value instanceof Boolean) {
                jsonObjectBuilder.add(inst.getName(), (boolean) value);
            } else if (value instanceof Integer) {
                jsonObjectBuilder.add(inst.getName(), (int) value);
            } else if (value instanceof Long) {
                jsonObjectBuilder.add(inst.getName(), (long) value);
            } else if (value instanceof Double) {
                jsonObjectBuilder.add(inst.getName(), (double) value);
            } else if (value instanceof BigDecimal) {
                jsonObjectBuilder.add(inst.getName(), (BigDecimal) value);
            } else if (value instanceof BigInteger) {
                jsonObjectBuilder.add(inst.getName(), (BigInteger) value);
            } else {
                if (DEFENDANTS.equalsIgnoreCase(inst.getName())) {
                    jsonObjectBuilder.add(inst.getName(), getJsonArrayBuilder(value.toString()));
                } else {
                    jsonObjectBuilder.add(inst.getName(), value.toString());
                }
            }
        }
    }

    protected JsonArrayBuilder getJsonArrayBuilder(final String value) {
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();

        try {
            final Defendant[] defendants = objectMapper.readValue(value, Defendant[].class);

            Arrays.stream(defendants).forEach(defendant -> {
                final JsonObject jsonObject = createObjectBuilder()
                        .add(ID, isNotEmpty(defendant.getId()) ? defendant.getId() : defendant.getDefendantId())
                        .add(FIRST_NAME, defendant.getFirstName())
                        .add(LAST_NAME, defendant.getLastName())
                        .build();
                jsonArrayBuilder.add(jsonObject);
            });
        } catch (IOException exception) {
            LOGGER.error("Unable to process defendants: ", exception);
        }

        return jsonArrayBuilder;
    }


    protected Object getValueOfVarInstanceVariable(final VariableInstance instance) {
        if (isNull(instance) || isNull(instance.getValue())) {
            return "";
        }

        if (instance.getTypeName().equalsIgnoreCase(PrimitiveValueType.DATE.getName())) {
            return dateFormat.format((Date) instance.getValue());
        }
        return instance.getValue();
    }
}