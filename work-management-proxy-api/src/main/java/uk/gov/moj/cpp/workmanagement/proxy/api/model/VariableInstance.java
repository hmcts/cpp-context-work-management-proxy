package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariableInstance {

    private String id;
    private String name;
    private String typeName;
    private Object value;
    private TypedValue typedValue;
    private String processInstanceId;
    private String executionId;
    private String processDefinitionId;
    private String caseInstanceId;
    private String caseExecutionId;
    private String taskId;
    private String batchId;
    private String activityInstanceId;
    private String errorMessage;
    private String tenantId;
}
