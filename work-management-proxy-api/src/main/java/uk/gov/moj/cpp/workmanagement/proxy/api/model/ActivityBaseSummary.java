package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class ActivityBaseSummary {

    protected Integer oneToSevenDaysCount;
    protected Integer eightToFourteenDaysCount;
    protected Integer fifteenToTwentyOneDaysCount;
    protected Integer overTwentyOneDaysCount;
    protected Integer total;
}
