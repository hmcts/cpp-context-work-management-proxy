package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowTaskType;
import uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

public class WorkflowTaskTypeMapper {
    private static final Logger LOGGER = getLogger(WorkflowTaskTypeMapper.class);

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private WorkflowTaskTypeMapper() {
    }

    public static List<WorkflowTaskType> mapToWorkflowTaskType(final Envelope<JsonObject> jsonObjectEnvelope) {
        if (nonNull(jsonObjectEnvelope)) {
            return mapToWorkflowTaskType(jsonObjectEnvelope.payload());
        }

        return Collections.emptyList();
    }

    public static List<WorkflowTaskType> mapToWorkflowTaskType(final JsonObject jsonObject) {
        if (nonNull(jsonObject)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(ContextConstants.WORK_FLOW_TASK_TYPES);

            if (nonNull(jsonArray)) {
                return jsonArray.stream()
                        .map(mapToWorkflowTaskType())
                        .toList();
            }
        }

        return Collections.emptyList();
    }

    private static Function<JsonValue, WorkflowTaskType> mapToWorkflowTaskType() {
        return jsonValue -> {
            try {
                return objectMapper.readValue(jsonValue.toString(), WorkflowTaskType.class);
            } catch (IOException ex) {
                LOGGER.error("Unable to unmarshal WorkflowTaskType. Payload :{}", jsonValue, ex);
                return null;
            }
        };
    }
}
