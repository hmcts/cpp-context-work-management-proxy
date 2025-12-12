package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowTaskType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReferenceDataServiceTest {
    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    private Envelope<JsonObject> envelope;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        JsonObject jsonObject = mock(JsonObject.class);
        envelope = mock(Envelope.class);
        when(envelope.payload()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);
    }

    @Test
    public void testGetWorkflowTaskTypes() {
        List<WorkflowTaskType> expectedList = Arrays.asList(
                createWorkflowTaskType(UUID.randomUUID(), "type1"),
                createWorkflowTaskType(UUID.randomUUID(), "type2")
        );

        try (MockedStatic<WorkflowTaskTypeMapper> mockedMapper = mockStatic(WorkflowTaskTypeMapper.class)) {
            mockedMapper.when(() -> WorkflowTaskTypeMapper.mapToWorkflowTaskType(envelope)).thenReturn(expectedList);

            List<WorkflowTaskType> result = referenceDataService.getWorkflowTaskTypes();
            assertEquals(expectedList, result);
        }
    }

    @Test
    public void testGetWorkflowTaskTypesWhenEmpty() {
        try (MockedStatic<WorkflowTaskTypeMapper> mockedMapper = mockStatic(WorkflowTaskTypeMapper.class)) {
            mockedMapper.when(() -> WorkflowTaskTypeMapper.mapToWorkflowTaskType(envelope)).thenReturn(Collections.emptyList());

            List<WorkflowTaskType> result = referenceDataService.getWorkflowTaskTypes();
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testGetWorkFlowTaskTypeWithId() {
        UUID id = UUID.randomUUID();
        WorkflowTaskType expectedTaskType = createWorkflowTaskType(id, "type1");

        List<WorkflowTaskType> workflowTaskTypes = Arrays.asList(
                expectedTaskType,
                createWorkflowTaskType(UUID.randomUUID(), "type2")
        );

        try (MockedStatic<WorkflowTaskTypeMapper> mockedMapper = mockStatic(WorkflowTaskTypeMapper.class)) {
            mockedMapper.when(() -> WorkflowTaskTypeMapper.mapToWorkflowTaskType(envelope)).thenReturn(workflowTaskTypes);

            WorkflowTaskType result = referenceDataService.getWorkFlowTaskTypeWithId(id);
            assertEquals(expectedTaskType, result);
        }
    }

    @Test
    public void testGetWorkFlowTaskTypeWithIdNotFound() {
        UUID id = UUID.randomUUID();

        List<WorkflowTaskType> workflowTaskTypes = Arrays.asList(
                createWorkflowTaskType(UUID.randomUUID(), "type1"),
                createWorkflowTaskType(UUID.randomUUID(), "type2")
        );

        try (MockedStatic<WorkflowTaskTypeMapper> mockedMapper = mockStatic(WorkflowTaskTypeMapper.class)) {
            mockedMapper.when(() -> WorkflowTaskTypeMapper.mapToWorkflowTaskType(envelope)).thenReturn(workflowTaskTypes);

            WorkflowTaskType result = referenceDataService.getWorkFlowTaskTypeWithId(id);
            assertNotNull(result);
            assertNull(result.getId());
            assertNull(result.getTaskName());
        }
    }

    private WorkflowTaskType createWorkflowTaskType(UUID id, String taskName) {
        String jsonString = String.format("{\"id\": \"%s\", \"taskName\": \"%s\"}", id, taskName);
        try {
            return objectMapper.readValue(jsonString, WorkflowTaskType.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create WorkflowTaskType from JSON", e);
        }
    }
}
