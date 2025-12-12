package uk.gov.moj.cpp.workmanagement.proxy.api.sorting;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.workmanagement.proxy.api.sorting.ActivityComparator.hearingDateComparator;
import static uk.gov.moj.cpp.workmanagement.proxy.api.sorting.ActivityComparator.urgentComparator;

import uk.gov.moj.cpp.workmanagement.proxy.api.mapper.TaskMapper;
import uk.gov.moj.cpp.workmanagement.proxy.api.mapper.VariableInstanceMapper;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskWithVariables;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;

public class DueDateComparatorTest extends BaseSortingTest {

    @Test
    public void sortActivity() {

        List<Task> tasks = createProcessInstanceTaskVariablesWithDueDates();

        final List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
                .taskIdIn(tasks.stream().map(Task::getId).toArray(String[]::new)).list();

        List<TaskWithVariables> taskWithVariables = tasks.stream().map(tk -> {
            TaskWithVariables ts = new TaskWithVariables(TaskMapper.TASK_MAPPER.taskToTaskDto(tk));
            ts.getVariableList().addAll(VariableInstanceMapper.VARIABLE_INSTANCE_MAPPER.variableInstancesToVariableInstancesDto(variables.stream().filter(var -> var.getTaskId().equalsIgnoreCase(tk.getId())).collect(toList())));
            return ts;
        }).sorted(urgentComparator).sorted(hearingDateComparator).collect(toList());

        List<String> actualData = taskWithVariables.stream().map(taskWithVariables1 -> taskWithVariables1.getTask().getName()).collect(Collectors.toList());

        assertThat(actualData, hasSize(10));
        assertEquals(Arrays.asList("Task-1-2", "Task-2-2", "Task-3-1", "Task-4-2", "Task-5-2", "Task-3-2", "Task-4-1", "Task-5-1", "Task-1-1", "Task-2-1"), actualData);
        prettyPrint(taskWithVariables);
    }

    private void prettyPrint(final List<TaskWithVariables> taskWithVariables) {
        LOGGER.info(String.format("%-20s%-30s%-30s%-30s\n", "Task Name", "DUE DATE", "HEARING DATE", "CREATE DATE"));
        for (TaskWithVariables tasks : taskWithVariables) {
            final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableList = tasks.getVariableList();
            final Map<String, Object> varMap = new HashMap<>();
            variableList.forEach(v -> varMap.put(v.getName(), v.getValue()));
            LOGGER.info(String.format("%-20s%-30s%-30s%-30s\n", tasks.getTask().getName(), tasks.getTask().getDueDate(), varMap.get(HEARING_DATE), tasks.getTask().getCreateTime()));
        }
    }

    private List<Task> createProcessInstanceTaskVariablesWithDueDates() {

        /*Create Task 1*/
        final List<Task> taskList1 = getTasks();

        final Task taskOne1 = taskList1.get(0);
        taskOne1.setName("Task-1-1");
        taskOne1.setDueDate(Date.from(now.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskOne1);
        final Map<String, Object> taskOneVariables1 = new HashMap<>();
        taskOneVariables1.put(HEARING_DATE, null);
        taskService.setVariablesLocal(taskOne1.getId(), taskOneVariables1);

        final Task taskOne2 = taskList1.get(1);
        taskOne2.setName("Task-1-2");
        taskOne2.setDueDate(Date.from(now.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskOne2);
        final Map<String, Object> taskOneVariables2 = new HashMap<>();
        taskOneVariables2.put(HEARING_DATE, Date.from(now.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskOne2.getId(), taskOneVariables2);

        /*Create Task 2*/
        final List<Task> taskList2 = getTasks();

        final Task taskTwo1 = taskList2.get(0);
        taskTwo1.setName("Task-2-1");
        taskTwo1.setDueDate(Date.from(now.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTwo1);
        final Map<String, Object> taskTwoVariables1 = new HashMap<>();
        taskTwoVariables1.put(HEARING_DATE, null);
        taskService.setVariablesLocal(taskTwo1.getId(), taskTwoVariables1);

        final Task taskTwo2 = taskList2.get(1);
        taskTwo2.setName("Task-2-2");
        taskTwo2.setDueDate(Date.from(now.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTwo2);
        final Map<String, Object> taskTwoVariables2 = new HashMap<>();
        taskTwoVariables2.put(HEARING_DATE, Date.from(now.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskTwo2.getId(), taskTwoVariables2);

        /*Create Task 3*/
        final List<Task> taskList3 = getTasks();

        final Task taskThree1 = taskList3.get(0);
        taskThree1.setName("Task-3-1");
        taskThree1.setDueDate(Date.from(now.plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskThree1);
        final Map<String, Object> taskThreeVariables1 = new HashMap<>();
        taskThreeVariables1.put(HEARING_DATE, Date.from(now.plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())); //
        taskService.setVariablesLocal(taskThree1.getId(), taskThreeVariables1);


        final Task taskThree2 = taskList3.get(1);
        taskThree2.setName("Task-3-2");
        taskThree2.setDueDate(Date.from(now.plusDays(3).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskThree2);
        final Map<String, Object> taskThreeVariables2 = new HashMap<>();
        taskThreeVariables2.put(HEARING_DATE, Date.from(now.minusDays(3).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskThree2.getId(), taskThreeVariables2);

        /*Create Task 4*/
        final List<Task> taskList4 = getTasks();

        final Task taskFour1 = taskList4.get(0);
        taskFour1.setName("Task-4-1");
        taskFour1.setDueDate(Date.from(now.plusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskFour1);
        final Map<String, Object> taskFourVariables1 = new HashMap<>();
        taskFourVariables1.put(HEARING_DATE, Date.from(now.minusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskFour1.getId(), taskFourVariables1);

        final Task taskFour2 = taskList4.get(1);
        taskFour2.setName("Task-4-2");
        taskFour2.setDueDate(Date.from(now.plusDays(-2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskFour2);
        final Map<String, Object> taskFourVariables2 = new HashMap<>();
        taskFourVariables2.put(HEARING_DATE, Date.from(now.minusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskFour2.getId(), taskFourVariables2);

        /*Create Task 5*/
        final List<Task> taskList5 = getTasks();

        final Task taskFive1 = taskList5.get(0);
        taskFive1.setName("Task-5-1");
        taskFive1.setDueDate(Date.from(now.plusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskFive1);
        final Map<String, Object> taskFiveVariables1 = new HashMap<>();
        taskFiveVariables1.put(HEARING_DATE, Date.from(now.minusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskFive1.getId(), taskFiveVariables1);


        final Task taskFive2 = taskList5.get(1);
        taskFive2.setName("Task-5-2");
        taskFive2.setDueDate(Date.from(now.plusDays(-2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskFive2);
        final Map<String, Object> taskFiveVariables2 = new HashMap<>();
        taskFiveVariables2.put(HEARING_DATE, Date.from(now.minusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskFive2.getId(), taskFiveVariables2);

        return Stream.of(taskList1, taskList2, taskList3, taskList4, taskList5)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}