package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class WorkflowTaskType {

    public WorkflowTaskType() {
    }

    public WorkflowTaskType(final UUID id) {
        this.id = id;
    }

    public WorkflowTaskType(UUID id, String organisationId, Integer seqNum, String taskName, String displayName, String taskGroup, Boolean isDeletable, Boolean isDeferrable, Integer duration, Integer followUpInterval, LocalDate validFrom, LocalDate validTo, String deepLink, String workQueueId, Boolean manualTask, String dueDateCalc, Boolean isCustomTask, String queueName, Boolean crownFlag, Boolean magistratesFlag, String caseId, String note, String caseTag) {
        this.id = id;
        this.organisationId = organisationId;
        this.seqNum = seqNum;
        this.taskName = taskName;
        this.displayName = displayName;
        this.taskGroup = taskGroup;
        this.isDeletable = isDeletable;
        this.isDeferrable = isDeferrable;
        this.duration = duration;
        this.followUpInterval = followUpInterval;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.deepLink = deepLink;
        this.workQueueId = workQueueId;
        this.manualTask = manualTask;
        this.dueDateCalc = dueDateCalc;
        this.isCustomTask = isCustomTask;
        this.queueName = queueName;
        this.crownFlag = crownFlag;
        this.magistratesFlag = magistratesFlag;
        this.caseId = caseId;
        this.note = note;
        this.caseTag = caseTag;
    }

    @JsonProperty("id")
    protected UUID id;

    @JsonProperty("organisationId")
    protected String organisationId;

    @JsonProperty("seqNum")
    protected Integer seqNum;

    @JsonProperty("taskName")
    protected String taskName;

    @JsonProperty("displayName")
    protected String displayName;

    @JsonProperty("taskGroup")
    protected String taskGroup;

    @JsonProperty("isDeletable")
    protected Boolean isDeletable;

    @JsonProperty("isDeferrable")
    protected Boolean isDeferrable;

    @JsonProperty("duration")
    protected Integer duration;

    @JsonProperty("followUpInterval")
    protected Integer followUpInterval;

    @JsonProperty("validFrom")
    protected LocalDate validFrom;

    @JsonProperty("validTo")
    protected LocalDate validTo;

    @JsonProperty("deepLink")
    protected String deepLink;

    @JsonProperty("workQueueId")
    protected String workQueueId;

    @JsonProperty("manualTask")
    protected Boolean manualTask;

    @JsonProperty("dueDateCalc")
    protected String dueDateCalc;

    @JsonProperty("isCustomTask")
    protected Boolean isCustomTask;

    @JsonProperty("queueName")
    protected String queueName;

    @JsonProperty("crownFlag")
    protected Boolean crownFlag;

    @JsonProperty("magistratesFlag")
    protected Boolean magistratesFlag;

    @JsonProperty("caseId")
    protected String caseId;

    @JsonProperty("note")
    protected String note;

    @JsonProperty("caseTag")
    protected String caseTag;

    public UUID getId() {
        return id;
    }

    public Integer getSeqNum() {
        return seqNum;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTaskGroup() {
        return taskGroup;
    }

    public Boolean getDeletable() {
        return isDeletable;
    }

    public Boolean getDeferrable() {
        return isDeferrable;
    }

    public Integer getDuration() {
        return duration;
    }

    public Integer getFollowUpInterval() {
        return followUpInterval;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public String getWorkQueueId() {
        return workQueueId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getNote() {
        return note;
    }

    public String getCaseTag() {
        return caseTag;
    }

    public Boolean getManualTask() {
        return manualTask;
    }

    public String getDueDateCalc() {
        return dueDateCalc;
    }

    public Boolean getCustomTask() {
        return isCustomTask;
    }

    public String getQueueName() {
        return queueName;
    }

    public Boolean getCrownFlag() {
        return crownFlag;
    }

    public Boolean getMagistratesFlag() {
        return magistratesFlag;
    }
}
