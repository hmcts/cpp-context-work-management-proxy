package uk.gov.moj.cpp.workmanagement.proxy.api.model;

public class Overall extends ActivityBaseSummary {

    public Overall(final Integer oneToSevenDaysCount, final Integer eightToFourteenDaysCount,
                   final Integer fifteenToTwentyOneDaysCount, final Integer overTwentyOneDaysCount,
                   final Integer total) {
        super(oneToSevenDaysCount, eightToFourteenDaysCount,
                fifteenToTwentyOneDaysCount, overTwentyOneDaysCount, total);
    }
}
