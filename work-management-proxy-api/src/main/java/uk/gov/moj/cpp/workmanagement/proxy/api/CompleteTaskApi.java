package uk.gov.moj.cpp.workmanagement.proxy.api;

import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.CANCELLATION_REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.COMPLETION_REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DELETED;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.DELETION_REASON;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LAST_UPDATED_BY_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.LAST_UPDATED_BY_NAME;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.TASK_ID;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.ContextConstants.USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.CustomServiceComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.UserService;

import javax.inject.Inject;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.exception.NullValueException;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CustomServiceComponent("WorkManagement.Proxy.API")
public class CompleteTaskApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteTaskApi.class);

    @Inject
    private UserService userService;

    @Inject
    private TaskService taskService;

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private ProcessEngine processEngine;

    /**
     * This is a workaround for performing task deletion.  Refer to ticket
     * https://tools.hmcts.net/jira/browse/DD-13310 for selection of this approach
     * <p>
     * The API call performs a local variable update indicating reason for deletion for the
     * specified task and completes the task.  The updated variable is then used by
     * TaskCompletedListener in businessprocesses context to determine the correct course of action
     * w.r.t auditing and public event generation for MI puropses
     *
     * @param envelope
     */
    @Handles("complete-task")
    public void completeTask(final JsonEnvelope envelope) {
        final String userId = envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException(USERID_CAN_NOT_BE_NULL_EXCEPTION_MESSAGE));

        final String taskId = envelope.payloadAsJsonObject().getString(TASK_ID);
        final String deletionReason = envelope.payloadAsJsonObject().getString(DELETION_REASON, null);
        final String completionReason = envelope.payloadAsJsonObject().getString(COMPLETION_REASON, null);
        final String cancellationReason = envelope.payloadAsJsonObject().getString(CANCELLATION_REASON, null);
        LOGGER.info("Invoking delete task API for task with ID '{}'", taskId);

        try {
            taskService.setVariableLocal(taskId, LAST_UPDATED_BY_ID, userId);
            final String userDetails = userService.getUserDetails(userId);
            if (userDetails != null) {
                taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userDetails);
            } else {
                taskService.setVariableLocal(taskId, LAST_UPDATED_BY_NAME, userId);
            }

            if (deletionReason != null) {
                taskService.setVariableLocal(taskId, DELETION_REASON, deletionReason);
                final Task updatedTask = taskService.createTaskQuery().taskId(taskId).singleResult();
                taskService.createComment(updatedTask.getId(), updatedTask.getProcessInstanceId(), deletionReason);

                runtimeService.suspendProcessInstanceById(updatedTask.getProcessInstanceId());
                runtimeService.deleteProcessInstance(updatedTask.getProcessInstanceId(), DELETED);
                taskService.deleteTask(taskId, DELETED);

            } else if (cancellationReason != null) {
                taskService.setVariableLocal(taskId, CANCELLATION_REASON, cancellationReason);
                taskService.complete(taskId);
            } else if (completionReason != null) {
                taskService.setVariableLocal(taskId, COMPLETION_REASON, completionReason);
                taskService.complete(taskId);
            } else {
                taskService.complete(taskId);
            }

        } catch (final NullValueException e) {
            LOGGER.warn("Error performing operation on task with ID '{}'", taskId);
            throw new BadRequestException(e.getMessage(), e);
        }
    }

}