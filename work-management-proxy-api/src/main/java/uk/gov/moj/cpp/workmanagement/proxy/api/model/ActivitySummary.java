package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummary {
    private List<Activity> activities;
    private int totalActivities;
}
