package uk.gov.moj.cpp.workmanagement.proxy.api.model;


import uk.gov.moj.cps.workmanagement.proxy.api.Defendant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@SuppressWarnings({"squid:S1161", "squid:S2384"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowCustomTaskType {

    private String taskName;
    private String taskType;
    private String due;
    private LocalDate hearingDate;
    private String note;
    private Boolean isUrgent;
    private Boolean isDeferrable;
    private Boolean isDeletable;
    private String displayName;
    private String assignee;
    private String caseURN;
    private String caseId;
    private List<Defendant> defendants;
    private String courtCodes;
    private String comment;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(final String taskName) {
        this.taskName = taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(final String taskType) {
        this.taskType = taskType;
    }

    public String getDue() {
        return due;
    }

    public void setDue(final String due) {
        this.due = due;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public void setHearingDate(final LocalDate hearingDate) {
        this.hearingDate = hearingDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public Boolean getUrgent() {
        return isUrgent;
    }

    public void setUrgent(final Boolean urgent) {
        isUrgent = urgent;
    }

    public Boolean getDeferrable() {
        return isDeferrable;
    }

    public void setDeferrable(final Boolean deferrable) {
        isDeferrable = deferrable;
    }

    public Boolean getDeletable() {
        return isDeletable;
    }

    public void setDeletable(final Boolean deletable) {
        isDeletable = deletable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(final String assignee) {
        this.assignee = assignee;
    }

    public String getCaseURN() {
        return caseURN;
    }

    public void setCaseURN(final String caseURN) {
        this.caseURN = caseURN;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(final String caseId) {
        this.caseId = caseId;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public void setDefendants(final List<Defendant> defendants) {
        this.defendants = defendants;
    }

    public String getCourtCodes() {
        return courtCodes;
    }

    public void setCourtCodes(final String courtCodes) {
        this.courtCodes = courtCodes;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "WorkflowCustomTaskType{" +
                "taskName='" + taskName + '\'' +
                ", taskType='" + taskType + '\'' +
                ", due=" + due +
                ", hearingDate=" + hearingDate +
                ", note='" + note + '\'' +
                ", isUrgent=" + isUrgent +
                ", isDeferrable=" + isDeferrable +
                ", isDeletable=" + isDeletable +
                ", displayName='" + displayName + '\'' +
                ", assignee='" + assignee + '\'' +
                ", caseURN='" + caseURN + '\'' +
                ", caseId='" + caseId + '\'' +
                ", defendants=" + defendants +
                ", courtCodes='" + courtCodes + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}