package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;

import java.util.Date;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"squid:S2384", "squid:S1213"})
public class TaskResponse {
    private String id;

    private String name;

    private String description;

    private int priority;

    private String owner;

    private String assignee;

    private String delegationState;

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

    private String formKey;

    private String tenantId;

    private Map<String, Object> variables;

    private int commentCount;

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public List<IdentityLink> getIdentityLinkList() {
        return identityLinkList;
    }

    public void setIdentityLinkList(final List<IdentityLink> identityLinkList) {
        this.identityLinkList = identityLinkList;
    }

    private List<IdentityLink> identityLinkList;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(final String assignee) {
        this.assignee = assignee;
    }

    public String getDelegationState() {
        return delegationState;
    }

    public void setDelegationState(final String delegationState) {
        this.delegationState = delegationState;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(final String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(final String executionId) {
        this.executionId = executionId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(final String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(final String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getCaseExecutionId() {
        return caseExecutionId;
    }

    public void setCaseExecutionId(final String caseExecutionId) {
        this.caseExecutionId = caseExecutionId;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(final String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final Date createTime) {
        this.createTime = createTime;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(final String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(final Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(final Date followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(final String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(final String formKey) {
        this.formKey = formKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(final String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(final Map<String, Object> variables) {
        this.variables = variables;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public static TaskResponse convertToResponse(TaskWithVariables taskWithVariables) {
        final TaskResponse taskResponse = new TaskResponse();
        taskResponse.setId(taskWithVariables.getTask().getId());
        taskResponse.setName(taskWithVariables.getTask().getName());
        taskResponse.setDescription(taskWithVariables.getTask().getDescription());
        taskResponse.setPriority(taskWithVariables.getTask().getPriority());
        taskResponse.setAssignee(taskWithVariables.getTask().getAssignee());
        taskResponse.setCaseDefinitionId(taskWithVariables.getTask().getCaseDefinitionId());
        taskResponse.setCaseExecutionId(taskWithVariables.getTask().getCaseExecutionId());
        taskResponse.setCaseInstanceId(taskWithVariables.getTask().getCaseInstanceId());
        taskResponse.setOwner(taskWithVariables.getTask().getOwner());
        taskResponse.setDelegationState(taskWithVariables.getTask().getDelegationState() != null ? taskWithVariables.getTask().getDelegationState().name() : null);
        taskResponse.setProcessInstanceId(taskWithVariables.getTask().getProcessInstanceId());
        taskResponse.setExecutionId(taskWithVariables.getTask().getExecutionId());
        taskResponse.setProcessDefinitionId(taskWithVariables.getTask().getProcessDefinitionId());
        taskResponse.setCreateTime(taskWithVariables.getTask().getCreateTime());
        taskResponse.setTaskDefinitionKey(taskWithVariables.getTask().getTaskDefinitionKey());
        taskResponse.setDueDate(taskWithVariables.getTask().getDueDate());
        taskResponse.setFollowUpDate(taskWithVariables.getTask().getFollowUpDate());
        taskResponse.setParentTaskId(taskWithVariables.getTask().getParentTaskId());
        taskResponse.setTenantId(taskWithVariables.getTask().getTenantId());
        taskResponse.setIdentityLinkList(taskWithVariables.getIdentityLinkList());
        taskResponse.setCommentCount(taskWithVariables.getCommentCount());
        taskResponse.setStatus(taskWithVariables.getTask().getDeleteReason());
        return taskResponse;
    }

}
