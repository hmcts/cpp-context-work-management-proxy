package uk.gov.moj.cpp.workmanagement.proxy.api.provider;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import uk.gov.moj.cpp.workmanagement.proxy.api.model.Activity;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.ActivityStat;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.ActivitySummary;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.Assigned;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.Escalated;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.Overall;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.Task;
import uk.gov.moj.cpp.workmanagement.proxy.api.model.Unassigned;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActivitySummaryProvider {

    private int oneToSevenDaysCount;
    private int eightToFourteenDaysCount;
    private int fifteenToTwentyOneDaysCount;
    private int overTwentyOneDaysCount;

    public ActivitySummary getActivitySummary(final List<Task> taskList) {
        final ActivitySummary activitySummary = new ActivitySummary();
        final List<Activity> activities = new ArrayList<>();
        final Map<String, List<Task>> tasks = taskList.stream()
                .collect(groupingBy(Task::getTaskDefinitionKey, LinkedHashMap::new, toList()));
        tasks.forEach((key, value) -> {
            final Activity currentActivity = new Activity();
            value.forEach(task -> setActivityStat(currentActivity, task));
            activities.add(currentActivity);
        });

        activitySummary.setActivities(activities);
        activitySummary.setTotalActivities(activities.size());
        return activitySummary;
    }

    private void setActivityStat(final Activity activity, final Task task) {
        ActivityStat activityStat;
        if (isNull(activity.getActivityStat())) {
            setActivityName(activity, task);
            activityStat = new ActivityStat();
        } else {
            activityStat = activity.getActivityStat();
        }
        setOverall(activityStat, task);
        setAssigned(activityStat, task);
        setUnAssigned(activityStat, task);
        setEscalated(activityStat, task);
        activity.setActivityStat(activityStat);
    }

    private void setActivityName(final Activity activity, final Task task) {
        if (isEmpty(activity.getTaskName())) {
            activity.setTaskName(task.getTaskDefinitionKey());
        }
    }

    private void setOverall(final ActivityStat activityStat, final Task task) {
        Overall overall;
        validateTaskCreationDate(task.getCreateTime(), Date.from(now(ZoneId.systemDefault()).toInstant()));
        if (isNull(activityStat.getOverall())) {
            overall = new Overall(oneToSevenDaysCount, eightToFourteenDaysCount, fifteenToTwentyOneDaysCount, overTwentyOneDaysCount,
                    oneToSevenDaysCount + eightToFourteenDaysCount + fifteenToTwentyOneDaysCount + overTwentyOneDaysCount);
            activityStat.setOverall(overall);
        } else {
            activityStat.getOverall().setOneToSevenDaysCount(activityStat.getOverall().getOneToSevenDaysCount() + oneToSevenDaysCount);
            activityStat.getOverall().setEightToFourteenDaysCount(activityStat.getOverall().getEightToFourteenDaysCount() + eightToFourteenDaysCount);
            activityStat.getOverall().setFifteenToTwentyOneDaysCount(activityStat.getOverall().getFifteenToTwentyOneDaysCount() + fifteenToTwentyOneDaysCount);
            activityStat.getOverall().setOverTwentyOneDaysCount(activityStat.getOverall().getOverTwentyOneDaysCount() + overTwentyOneDaysCount);
            activityStat.getOverall().setTotal(activityStat.getOverall().getOneToSevenDaysCount()
                    + activityStat.getOverall().getEightToFourteenDaysCount()
                    + activityStat.getOverall().getFifteenToTwentyOneDaysCount()
                    + activityStat.getOverall().getOverTwentyOneDaysCount());
        }
    }


    private void setAssigned(final ActivityStat activityStat, final Task task) {
        Assigned assigned;
        validateTaskCreationDate(task.getCreateTime(), Date.from(now(ZoneId.systemDefault()).toInstant()));
        if (isNull(activityStat.getAssigned())) {
            assigned = new Assigned(0, 0, 0, 0, 0);
            activityStat.setAssigned(assigned);
        }
        if (isNoneEmpty(task.getAssignee())) {
            if (isNull(activityStat.getAssigned())) {
                assigned = new Assigned(oneToSevenDaysCount, eightToFourteenDaysCount, fifteenToTwentyOneDaysCount, overTwentyOneDaysCount,
                        oneToSevenDaysCount + eightToFourteenDaysCount + fifteenToTwentyOneDaysCount + overTwentyOneDaysCount);
                activityStat.setAssigned(assigned);
            } else {
                activityStat.getAssigned().setOneToSevenDaysCount(activityStat.getAssigned().getOneToSevenDaysCount() + oneToSevenDaysCount);
                activityStat.getAssigned().setEightToFourteenDaysCount(activityStat.getAssigned().getEightToFourteenDaysCount() + eightToFourteenDaysCount);
                activityStat.getAssigned().setFifteenToTwentyOneDaysCount(activityStat.getAssigned().getFifteenToTwentyOneDaysCount() + fifteenToTwentyOneDaysCount);
                activityStat.getAssigned().setOverTwentyOneDaysCount(activityStat.getAssigned().getOverTwentyOneDaysCount() + overTwentyOneDaysCount);
                activityStat.getAssigned().setTotal(activityStat.getAssigned().getOneToSevenDaysCount()
                        + activityStat.getAssigned().getEightToFourteenDaysCount()
                        + activityStat.getAssigned().getFifteenToTwentyOneDaysCount()
                        + activityStat.getAssigned().getOverTwentyOneDaysCount());
            }
        }
    }

    private void setUnAssigned(final ActivityStat activityStat, final Task task) {
        Unassigned unassigned;
        validateTaskCreationDate(task.getCreateTime(), Date.from(now(ZoneId.systemDefault()).toInstant()));
        if (isNull(activityStat.getUnassigned())) {
            unassigned = new Unassigned(0, 0, 0, 0, 0);
            activityStat.setUnassigned(unassigned);
        }
        if (isEmpty(task.getAssignee())) {
            if (isNull(activityStat.getUnassigned())) {
                unassigned = new Unassigned(oneToSevenDaysCount, eightToFourteenDaysCount, fifteenToTwentyOneDaysCount, overTwentyOneDaysCount,
                        oneToSevenDaysCount + eightToFourteenDaysCount + fifteenToTwentyOneDaysCount + overTwentyOneDaysCount);
                activityStat.setUnassigned(unassigned);
            } else {
                activityStat.getUnassigned().setOneToSevenDaysCount(activityStat.getUnassigned().getOneToSevenDaysCount() + oneToSevenDaysCount);
                activityStat.getUnassigned().setEightToFourteenDaysCount(activityStat.getUnassigned().getEightToFourteenDaysCount() + eightToFourteenDaysCount);
                activityStat.getUnassigned().setFifteenToTwentyOneDaysCount(activityStat.getUnassigned().getFifteenToTwentyOneDaysCount() + fifteenToTwentyOneDaysCount);
                activityStat.getUnassigned().setOverTwentyOneDaysCount(activityStat.getUnassigned().getOverTwentyOneDaysCount() + overTwentyOneDaysCount);
                activityStat.getUnassigned().setTotal(activityStat.getUnassigned().getOneToSevenDaysCount()
                        + activityStat.getUnassigned().getEightToFourteenDaysCount()
                        + activityStat.getUnassigned().getFifteenToTwentyOneDaysCount()
                        + activityStat.getUnassigned().getOverTwentyOneDaysCount());
            }
        }
    }

    private void setEscalated(final ActivityStat activityStat, final Task task) {
        Escalated escalated;
        validateTaskCreationDate(task.getCreateTime(), Date.from(now(ZoneId.systemDefault()).toInstant()));
        if (isNull(activityStat.getEscalated())) {
            escalated = new Escalated(0, 0, 0, 0, 0);
            activityStat.setEscalated(escalated);
        }
        if (task.getCreateTime().after(Date.from(now(ZoneId.systemDefault()).toInstant()))) {
            if (isNull(activityStat.getEscalated())) {
                escalated = new Escalated(oneToSevenDaysCount, eightToFourteenDaysCount, fifteenToTwentyOneDaysCount, overTwentyOneDaysCount,
                        oneToSevenDaysCount + eightToFourteenDaysCount + fifteenToTwentyOneDaysCount + overTwentyOneDaysCount);
                activityStat.setEscalated(escalated);
            } else {
                activityStat.getEscalated().setOneToSevenDaysCount(activityStat.getEscalated().getOneToSevenDaysCount() + oneToSevenDaysCount);
                activityStat.getEscalated().setEightToFourteenDaysCount(activityStat.getEscalated().getEightToFourteenDaysCount() + eightToFourteenDaysCount);
                activityStat.getEscalated().setFifteenToTwentyOneDaysCount(activityStat.getEscalated().getFifteenToTwentyOneDaysCount() + fifteenToTwentyOneDaysCount);
                activityStat.getEscalated().setOverTwentyOneDaysCount(activityStat.getEscalated().getOverTwentyOneDaysCount() + overTwentyOneDaysCount);
                activityStat.getEscalated().setTotal(activityStat.getEscalated().getOneToSevenDaysCount()
                        + activityStat.getEscalated().getEightToFourteenDaysCount()
                        + activityStat.getEscalated().getFifteenToTwentyOneDaysCount()
                        + activityStat.getEscalated().getOverTwentyOneDaysCount());
            }
        }
    }

    private void validateTaskCreationDate(final Date taskCreateTime, final Date today) {
        resetValues();
        final long numberOfDays = DAYS.between(taskCreateTime.toInstant(), today.toInstant());
        if (numberOfDays < 7) {
            oneToSevenDaysCount = 1;
        } else if (numberOfDays < 14) {
            eightToFourteenDaysCount = 1;
        } else if (numberOfDays < 21) {
            fifteenToTwentyOneDaysCount = 1;
        } else {
            overTwentyOneDaysCount = 1;
        }
    }

    private void resetValues() {
        oneToSevenDaysCount = 0;
        eightToFourteenDaysCount = 0;
        fifteenToTwentyOneDaysCount = 0;
        overTwentyOneDaysCount = 0;
    }
}