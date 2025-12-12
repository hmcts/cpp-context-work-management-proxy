package uk.gov.moj.cpp.workmanagement.proxy.api.sorting;

import static java.lang.Boolean.compare;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils.getDueDate;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils.getHearingDate;
import static uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils.isUrgent;

import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;

import java.time.LocalDate;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityComparator {

    private static final LocalDate now = LocalDate.now();

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityComparator.class);
    public static final Comparator<TaskWithVariables> urgentComparator = (tv1, tv2) ->
            compare(isUrgent(tv2), isUrgent(tv1));

    public static final Comparator<TaskWithVariables> dueDateComparator = (tv1, tv2) -> {
        try {
            final LocalDate dueDate1 = nonNull(getDueDate(tv1)) ? new java.sql.Date(getDueDate(tv1).getTime()).toLocalDate() : null;
            final LocalDate dueDate2 = nonNull(getDueDate(tv2)) ? new java.sql.Date(getDueDate(tv2).getTime()).toLocalDate() : null;
            return returnDueDateComparison(dueDate1, dueDate2);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            LOGGER.error("Due date comparing error ", e);
            return -1;
        }
    };

    public static final Comparator<TaskWithVariables> hearingDateComparator = (tv1, tv2) -> {
        final LocalDate hearingDate1 = getHearingDateFormatted(tv1);
        final LocalDate hearingDate2 = getHearingDateFormatted(tv2);
        final Integer checkNullValue = checkDate(hearingDate1, hearingDate2);
        if (checkNullValue != null) {
            return checkNullValue;
        }
        if ((hearingDate1.isBefore(now.minusDays(1)) && isDateInGivenRangeInclusiveOfWholeRange(hearingDate2))) {
            return 1;
        } else if ((hearingDate2.isBefore(now.minusDays(1)) && isDateInGivenRangeInclusiveOfWholeRange(hearingDate1))) {
            return -1;
        }
        if (isDateInGivenRangeInclusiveOfWholeRange(hearingDate1) && hearingDate2.isAfter(now.plusDays(6))) {
            return -1;
        } else if (isDateInGivenRangeInclusiveOfWholeRange(hearingDate2) && hearingDate1.isAfter(now.plusDays(6))) {
            return 1;
        }
        return hearingDate1.compareTo(hearingDate2);
    };

    private ActivityComparator() {
    }

    private static boolean isDateInGivenRangeInclusiveOfWholeRange(LocalDate input) {
        return input.isAfter(ActivityComparator.now.minusDays(1)) && input.isBefore(ActivityComparator.now.plusDays(6));
    }

    private static Integer checkDate(final LocalDate date1, final LocalDate date2) {
        if (isNull(date1) && isNull(date2)) {
            return 0;
        }
        if (isNull(date1)) {
            return 1;
        }
        if (isNull(date2)) {
            return -1;
        }
        return null;
    }

    private static LocalDate getHearingDateFormatted(TaskWithVariables task) {
        try {
            return nonNull(getHearingDate(task)) ? new java.sql.Date(getHearingDate(task).getTime()).toLocalDate() : null;
        } catch (IllegalArgumentException | ClassCastException ex) {
            LOGGER.error("Hearing date format problem", ex);
            return null;
        }
    }

    private static Integer returnDueDateComparison(final LocalDate dueDate1, final LocalDate dueDate2) {
        if (isNull(dueDate1) && isNull(dueDate2)) {
            return 0;
        }
        if (isNull(dueDate1)) {
            return -1;
        }
        if (isNull(dueDate2)) {
            return 1;
        }
        return dueDate1.compareTo(dueDate2);
    }

}
