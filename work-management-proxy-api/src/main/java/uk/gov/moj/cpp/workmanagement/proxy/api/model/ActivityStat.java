package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActivityStat {

    private Escalated escalated;
    private Overall overall;
    private Assigned assigned;
    private Unassigned unassigned;
}
