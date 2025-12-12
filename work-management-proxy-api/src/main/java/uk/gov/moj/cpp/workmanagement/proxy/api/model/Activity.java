package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    private String taskName;
    private String displayName;
    private ActivityStat activityStat;
}