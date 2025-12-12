package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityLink {
    private String id;
    private String type;
    private String userId;
    private String groupId;
    private String taskId;
    private String processDefId;
    private String tenantId;
}
