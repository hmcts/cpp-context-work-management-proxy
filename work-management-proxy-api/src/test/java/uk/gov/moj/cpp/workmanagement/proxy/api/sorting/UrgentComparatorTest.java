package uk.gov.moj.cpp.workmanagement.proxy.api.sorting;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class UrgentComparatorTest extends BaseSortingTest {

    @Test
    public void sortActivity() {

        final List<Task> tasks = createProcessInstanceTaskVariablesWithUrgentData();

        final List<VariableInstance> variables = runtimeService.createVariableInstanceQuery()
                .taskIdIn(tasks.stream().map(Task::getId).toArray(String[]::new)).list();

        List<TaskWithVariables> taskWithVariables = tasks.stream().map(tk -> {
            TaskWithVariables ts = new TaskWithVariables(TaskMapper.TASK_MAPPER.taskToTaskDto(tk));
            ts.getVariableList().addAll(VariableInstanceMapper.VARIABLE_INSTANCE_MAPPER.variableInstancesToVariableInstancesDto(variables.stream().filter(var -> var.getTaskId().equalsIgnoreCase(tk.getId())).collect(toList())));
            return ts;
        }).collect(toList());

        SortActivity sortActivity = new SortActivity();
        sortActivity.sort(taskWithVariables);

        List<String> actualData = taskWithVariables.stream().map(taskWithVariables1 -> taskWithVariables1.getTask().getName()).collect(Collectors.toList());

        assertThat(actualData, hasSize(22));
        assertEquals(Arrays.asList("Task-10-2", "Task-10-1", "Task-9-2", "Task-9-1", "Task-8-2", "Task-8-1", "Task-7-2", "Task-7-1", "Task-6-2", "Task-6-1", "Task-5-2", "Task-5-1", "Task-4-2", "Task-4-1", "Task-3-2", "Task-3-1", "Task-2-2", "Task-2-1", "Task-1-2", "Task-1-1", "Task-11-1", "Task-11-2"), actualData);

        prettyPrint(taskWithVariables);
    }

    private void prettyPrint(final List<TaskWithVariables> taskWithVariables) {
        LOGGER.info(String.format("%-20s%-15s%-30s%-30s%-30s\n", "Task Name", "IS URGENT", "DUE DATE", "HEARING DATE", "CREATE DATE"));
        for (TaskWithVariables tasks : taskWithVariables) {
            final List<uk.gov.moj.cpp.workmanagement.proxy.api.model.VariableInstance> variableList = tasks.getVariableList();
            final Map<String, Object> varMap = new HashMap<>();
            variableList.forEach(v -> varMap.put(v.getName(), v.getValue()));
            LOGGER.info(String.format("%-20s%-15s%-30s%-30s%-30s\n", tasks.getTask().getName(), varMap.get(IS_URGENT), tasks.getTask().getDueDate(), varMap.get(HEARING_DATE), tasks.getTask().getCreateTime()));
        }
    }

    private List<Task> createProcessInstanceTaskVariablesWithUrgentData() {

        /*Create Task 1*/
        final List<Task> taskList1 = getTasks();

        final Task taskOne1 = taskList1.get(0);
        taskOne1.setName("Task-1-1");
        taskOne1.setDueDate(Date.from(now.plusDays(20).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskOne1);
        final Map<String, Object> taskOneVariables1 = new HashMap<>();
        taskOneVariables1.put(IS_URGENT, Boolean.TRUE);
        taskOneVariables1.put(HEARING_DATE, null);
        taskService.setVariablesLocal(taskOne1.getId(), taskOneVariables1);

        final Task taskOne2 = taskList1.get(1);
        taskOne2.setName("Task-1-2");
        taskOne2.setDueDate(Date.from(now.plusDays(19).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskOne2);
        final Map<String, Object> taskOneVariables2 = new HashMap<>();
        taskOneVariables2.put(IS_URGENT, Boolean.TRUE);
        taskOneVariables2.put(HEARING_DATE, Date.from(now.minusDays(19).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskOne2.getId(), taskOneVariables2);

        /*Create Task 2*/
        final List<Task> taskList2 = getTasks();

        final Task taskTwo1 = taskList2.get(0);
        taskTwo1.setName("Task-2-1");
        taskTwo1.setDueDate(Date.from(now.plusDays(18).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTwo1);
        final Map<String, Object> taskTwoVariables1 = new HashMap<>();
        taskTwoVariables1.put(IS_URGENT, Boolean.TRUE);
        taskTwoVariables1.put(HEARING_DATE, "null");
        taskService.setVariablesLocal(taskTwo1.getId(), taskTwoVariables1);

        final Task taskTwo2 = taskList2.get(1);
        taskTwo2.setName("Task-2-2");
        taskTwo2.setDueDate(Date.from(now.plusDays(17).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTwo2);
        final Map<String, Object> taskTwoVariables2 = new HashMap<>();
        taskTwoVariables2.put(IS_URGENT, Boolean.TRUE);
        taskTwoVariables2.put(HEARING_DATE, Date.from(now.minusDays(18).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskTwo2.getId(), taskTwoVariables2);

        /*Create Task 3*/
        final List<Task> taskList3 = getTasks();

        final Task taskThree1 = taskList3.get(0);
        taskThree1.setName("Task-3-1");
        taskThree1.setDueDate(Date.from(now.plusDays(16).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskThree1);
        final Map<String, Object> taskThreeVariables1 = new HashMap<>();
        taskThreeVariables1.put(IS_URGENT, Boolean.TRUE);
        taskThreeVariables1.put(HEARING_DATE, Date.from(now.plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())); //
        taskService.setVariablesLocal(taskThree1.getId(), taskThreeVariables1);


        final Task taskThree2 = taskList3.get(1);
        taskThree2.setName("Task-3-2");
        taskThree2.setDueDate(Date.from(now.plusDays(15).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskThree2);
        final Map<String, Object> taskThreeVariables2 = new HashMap<>();
        taskThreeVariables2.put(IS_URGENT, Boolean.TRUE);
        taskThreeVariables2.put(HEARING_DATE, Date.from(now.minusDays(3).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskThree2.getId(), taskThreeVariables2);

        /*Create Task 4*/
        final List<Task> taskList4 = getTasks();

        final Task taskFour1 = taskList4.get(0);
        taskFour1.setName("Task-4-1");
        taskFour1.setDueDate(Date.from(now.plusDays(14).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskFour1);
        final Map<String, Object> taskFourVariables1 = new HashMap<>();
        taskFourVariables1.put(IS_URGENT, Boolean.TRUE);
        taskFourVariables1.put(HEARING_DATE, Date.from(now.minusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskFour1.getId(), taskFourVariables1);

        final Task taskFour2 = taskList4.get(1);
        taskFour2.setName("Task-4-2");
        taskFour2.setDueDate(Date.from(now.plusDays(13).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskFour2);
        final Map<String, Object> taskFourVariables2 = new HashMap<>();
        taskFourVariables2.put(IS_URGENT, Boolean.TRUE);
        taskFourVariables2.put(HEARING_DATE, Date.from(now.minusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskFour2.getId(), taskFourVariables2);

        /*Create Task 5*/
        final List<Task> taskList5 = getTasks();

        final Task taskFive1 = taskList5.get(0);
        taskFive1.setName("Task-5-1");
        taskFive1.setDueDate(Date.from(now.plusDays(12).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskFive1);
        final Map<String, Object> taskFiveVariables1 = new HashMap<>();
        taskFiveVariables1.put(IS_URGENT, Boolean.TRUE);
        taskFiveVariables1.put(HEARING_DATE, Date.from(now.minusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskFive1.getId(), taskFiveVariables1);


        final Task taskFive2 = taskList5.get(1);
        taskFive2.setName("Task-5-2");
        taskFive2.setDueDate(Date.from(now.plusDays(11).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskFive2);
        final Map<String, Object> taskFiveVariables2 = new HashMap<>();
        taskFiveVariables2.put(IS_URGENT, Boolean.TRUE);
        taskFiveVariables2.put(HEARING_DATE, Date.from(now.minusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskFive2.getId(), taskFiveVariables2);

        /*Create Task 6*/
        final List<Task> taskList6 = getTasks();

        final Task taskSix1 = taskList6.get(0);
        taskSix1.setName("Task-6-1");
        taskSix1.setDueDate(Date.from(now.plusDays(10).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskSix1);
        final Map<String, Object> taskSixVariables1 = new HashMap<>();
        taskSixVariables1.put(IS_URGENT, Boolean.TRUE);
        taskSixVariables1.put(HEARING_DATE, null);
        taskService.setVariablesLocal(taskSix1.getId(), taskSixVariables1);

        final Task taskSix2 = taskList6.get(1);
        taskSix2.setName("Task-6-2");
        taskSix2.setDueDate(Date.from(now.plusDays(9).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskSix2);
        final Map<String, Object> taskSixVariables2 = new HashMap<>();
        taskSixVariables2.put(IS_URGENT, Boolean.TRUE);
        taskSixVariables2.put(HEARING_DATE, Date.from(now.minusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskSix2.getId(), taskSixVariables2);

        /*Create Task 7*/
        final List<Task> taskList7 = getTasks();

        final Task taskSeven1 = taskList7.get(0);
        taskSeven1.setName("Task-7-1");
        taskSeven1.setDueDate(Date.from(now.plusDays(8).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskSeven1);
        final Map<String, Object> taskSevenVariables1 = new HashMap<>();
        taskSevenVariables1.put(IS_URGENT, Boolean.TRUE);
        taskSevenVariables1.put(HEARING_DATE, Date.from(now.minusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskSeven1.getId(), taskSevenVariables1);

        final Task taskSeven2 = taskList7.get(1);
        taskSeven2.setName("Task-7-2");
        taskSeven2.setDueDate(Date.from(now.plusDays(7).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskSeven2);
        final Map<String, Object> taskSevenVariables2 = new HashMap<>();
        taskSevenVariables2.put(IS_URGENT, Boolean.TRUE);
        taskSevenVariables2.put(HEARING_DATE, Date.from(now.minusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskSeven2.getId(), taskSevenVariables2);

        /*Create Task 8*/
        final List<Task> taskList8 = getTasks();

        final Task taskEight1 = taskList8.get(0);
        taskEight1.setName("Task-8-1");
        taskEight1.setDueDate(Date.from(now.plusDays(6).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskEight1);
        final Map<String, Object> taskEightVariables1 = new HashMap<>();
        taskEightVariables1.put(IS_URGENT, Boolean.TRUE);
        taskEightVariables1.put(HEARING_DATE, null);
        taskService.setVariablesLocal(taskEight1.getId(), taskEightVariables1);

        final Task taskEight2 = taskList8.get(1);
        taskEight2.setName("Task-8-2");
        taskEight2.setDueDate(Date.from(now.plusDays(5).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskEight2);
        final Map<String, Object> taskEightVariables2 = new HashMap<>();
        taskEightVariables2.put(IS_URGENT, Boolean.TRUE);
        taskEightVariables2.put(HEARING_DATE, Date.from(now.minusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskEight2.getId(), taskEightVariables2);

        /*Create Task 9*/
        final List<Task> taskList9 = getTasks();

        final Task taskNine1 = taskList9.get(0);
        taskNine1.setName("Task-9-1");
        taskNine1.setDueDate(Date.from(now.plusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskNine1);
        final Map<String, Object> taskNineVariables1 = new HashMap<>();
        taskNineVariables1.put(IS_URGENT, Boolean.TRUE);
        taskNineVariables1.put(HEARING_DATE, Date.from(now.minusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskNine1.getId(), taskNineVariables1);

        final Task taskNine2 = taskList9.get(1);
        taskNine2.setName("Task-9-2");
        taskNine2.setDueDate(Date.from(now.plusDays(3).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskNine2);
        final Map<String, Object> taskNineVariables2 = new HashMap<>();
        taskNineVariables2.put(HEARING_DATE, Date.from(now.minusDays(3).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskNineVariables2.put(IS_URGENT, Boolean.TRUE);
        taskService.setVariablesLocal(taskNine2.getId(), taskNineVariables2);

        /*Create Task 10*/
        final List<Task> taskList10 = getTasks();

        final Task taskTen1 = taskList10.get(0);
        taskTen1.setName("Task-10-1");
        taskTen1.setDueDate(Date.from(now.plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTen1);
        final Map<String, Object> taskTenVariables1 = new HashMap<>();
        taskTenVariables1.put(IS_URGENT, Boolean.TRUE);
        taskTenVariables1.put(HEARING_DATE, Date.from(now.plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskTen1.getId(), taskTenVariables1);

        final Task taskTen2 = taskList10.get(1);
        taskTen2.setName("Task-10-2");
        taskTen2.setDueDate(Date.from(now.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskTen2);
        final Map<String, Object> taskTenVariables2 = new HashMap<>();
        taskTenVariables2.put(IS_URGENT, Boolean.TRUE);
        taskTenVariables2.put(HEARING_DATE, Date.from(now.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskTen2.getId(), taskTenVariables2);

        /*Create Task 11*/
        final List<Task> taskList11 = getTasks();

        final Task taskEleven1 = taskList11.get(0);
        taskEleven1.setName("Task-11-1");
        taskEleven1.setDueDate(Date.from(now.plusDays(3).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskEleven1);
        final Map<String, Object> taskElevenVariables1 = new HashMap<>();
        taskElevenVariables1.put(IS_URGENT, Boolean.FALSE);
        taskElevenVariables1.put(HEARING_DATE, Date.from(now.plusDays(6).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskEleven1.getId(), taskElevenVariables1);

        final Task taskEleven2 = taskList11.get(1);
        taskEleven2.setName("Task-11-2");
        taskEleven2.setDueDate(Date.from(now.plusDays(4).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.saveTask(taskEleven2);
        final Map<String, Object> taskElevenVariables2 = new HashMap<>();
        taskElevenVariables2.put(IS_URGENT, Boolean.FALSE);
        taskElevenVariables2.put(HEARING_DATE, Date.from(now.plusDays(7).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        taskService.setVariablesLocal(taskEleven2.getId(), taskElevenVariables2);

        return Stream.of(taskList1, taskList2, taskList3, taskList4, taskList5, taskList6, taskList7, taskList8, taskList9, taskList10, taskList11)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}