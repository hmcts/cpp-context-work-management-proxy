package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.workmanagement.proxy.api.service.WorkflowTaskTypeMapper.mapToWorkflowTaskType;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.WorkflowTaskType;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {
    private static final String REFERENCE_DATA_QUERY_WORKFLOW_TASK_TYPES = "referencedata.query.workflow-task-types";

    @Inject
    @ServiceComponent("WorkManagement.Proxy.API")
    private Requester requester;

    public List<WorkflowTaskType> getWorkflowTaskTypes() {
        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCE_DATA_QUERY_WORKFLOW_TASK_TYPES);
        final Envelope<JsonObject> envelope = requester.requestAsAdmin(envelopeFrom(metadataBuilder, createObjectBuilder().build()), JsonObject.class);

        return mapToWorkflowTaskType(envelope);
    }

    public WorkflowTaskType getWorkFlowTaskTypeWithId(final UUID id) {
        final List<WorkflowTaskType> workflowTaskTypes = getWorkflowTaskTypes();
        return workflowTaskTypes.stream().filter(wtt -> wtt.getId().equals(id)).findFirst().orElse(new WorkflowTaskType());
    }
}
