package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import uk.gov.moj.cpp.workmanagement.proxy.api.model.IdentityLink;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.Task;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"squid:S2384"})
public class TaskWithVariables {

    private Task task;

    private int commentCount;
    private List<VariableInstance> variableList;
    private Map<String, Object> variableMap;
    private List<IdentityLink> identityLinkList;

    public TaskWithVariables(final Task task) {
        this.task = task;
        this.variableList = new ArrayList<>();
        this.identityLinkList = new ArrayList<>();
    }

    public Task getTask() {
        return task;
    }

    public void setTask(final Task task) {
        this.task = task;
    }

    public List<VariableInstance> getVariableList() {
        return variableList;
    }

    public Map<String, Object> getVariableMap() {
        return variableMap;
    }

    public void setVariableList(final List<VariableInstance> variableList) {
        this.variableList = variableList;
    }
    public void setVariableMap(final Map<String, Object> variableMap) {
        this.variableMap = variableMap;
    }

    public List<IdentityLink> getIdentityLinkList() {
        return identityLinkList;
    }

    public void setIdentityLinkList(final List<IdentityLink> identityLinkList) {
        this.identityLinkList = identityLinkList;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
}
