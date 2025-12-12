package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private String id;
    private String name;
    private String description;
    private int priority;
    private String owner;
    private String assignee;
    private DelegationState delegationState;
    private String processInstanceId;
    private String executionId;
    private String processDefinitionId;
    private String caseInstanceId;
    private String caseExecutionId;
    private String caseDefinitionId;
    private Date createTime;
    private String taskDefinitionKey;
    private Date dueDate;
    private Date followUpDate;
    private String parentTaskId;
    private boolean isSuspended;
    private String tenantId;
    private String deleteReason;
}
