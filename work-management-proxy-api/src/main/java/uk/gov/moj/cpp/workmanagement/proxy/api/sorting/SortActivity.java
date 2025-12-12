package uk.gov.moj.cpp.workmanagement.proxy.api.sorting;

import static uk.gov.moj.cpp.workmanagement.proxy.api.sorting.ActivityComparator.dueDateComparator;

import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;
import uk.gov.moj.cpp.workmanagement.proxy.api.util.SortingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"squid:S1854", "squid:S1481"})
public class SortActivity {

/*  This method sorts the taskWithVariablesList based on the due date and urgency of the task.
    The tasks are sorted in the following order:
    1. Overdue tasks
    2. Urgent tasks
    3. Tasks that are neither overdue nor urgent   */
    public void sort(final List<TaskWithVariables> taskWithVariablesList) {
        List<TaskWithVariables> orderedTaskList = new ArrayList<>();
        List<TaskWithVariables> overDueTasks = taskWithVariablesList.stream()
                .filter(SortingUtils::isOverDue)
                .sorted(dueDateComparator)
                .toList();
        orderedTaskList.addAll(overDueTasks);
        taskWithVariablesList.removeAll(overDueTasks);

        List<TaskWithVariables> urgentTasks = taskWithVariablesList.stream()
                .filter(SortingUtils::isUrgent)
                .sorted(dueDateComparator)
                .toList();
        orderedTaskList.addAll(urgentTasks);
        taskWithVariablesList.removeAll(urgentTasks);

        taskWithVariablesList.sort(dueDateComparator);
        orderedTaskList.addAll(taskWithVariablesList);

        taskWithVariablesList.clear();
        taskWithVariablesList.addAll(orderedTaskList);
     }
}
