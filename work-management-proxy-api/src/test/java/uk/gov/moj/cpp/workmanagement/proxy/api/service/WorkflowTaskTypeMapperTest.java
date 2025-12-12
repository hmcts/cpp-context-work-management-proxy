package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowTaskType;
import uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkflowTaskTypeMapperTest {

    @Test
    public void testMapToWorkflowTaskTypeWithValidEnvelope() {
        JsonObject jsonObject = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonValue jsonValue = mock(JsonValue.class);

        when(jsonObject.getJsonArray(ContextConstants.WORK_FLOW_TASK_TYPES)).thenReturn(jsonArray);
        when(jsonArray.stream()).thenReturn(List.of(jsonValue).stream());

        Envelope<JsonObject> envelope = mock(Envelope.class);
        when(envelope.payload()).thenReturn(jsonObject);

        List<WorkflowTaskType> result = WorkflowTaskTypeMapper.mapToWorkflowTaskType(envelope);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testMapToWorkflowTaskTypeWithNullEnvelope() {
        List<WorkflowTaskType> result = WorkflowTaskTypeMapper.mapToWorkflowTaskType((Envelope<JsonObject>) null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapToWorkflowTaskTypeWithNullJsonObject() {
        List<WorkflowTaskType> result = WorkflowTaskTypeMapper.mapToWorkflowTaskType((JsonObject) null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapToWorkflowTaskTypeWithValidJsonObject() {
        JsonObject jsonObject = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);
        JsonValue jsonValue = mock(JsonValue.class);

        when(jsonObject.getJsonArray(ContextConstants.WORK_FLOW_TASK_TYPES)).thenReturn(jsonArray);
        when(jsonArray.stream()).thenReturn(List.of(jsonValue).stream());

        List<WorkflowTaskType> result = WorkflowTaskTypeMapper.mapToWorkflowTaskType(jsonObject);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testMapToWorkflowTaskTypeWithEmptyJsonArray() {
        JsonObject jsonObject = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        when(jsonObject.getJsonArray(ContextConstants.WORK_FLOW_TASK_TYPES)).thenReturn(jsonArray);

        List<WorkflowTaskType> result = WorkflowTaskTypeMapper.mapToWorkflowTaskType(jsonObject);

        assertTrue(result.isEmpty());
    }
}
