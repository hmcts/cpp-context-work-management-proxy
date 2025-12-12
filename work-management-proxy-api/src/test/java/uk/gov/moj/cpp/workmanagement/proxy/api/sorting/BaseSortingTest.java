package uk.gov.moj.cpp.workmanagement.proxy.api.sorting;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.CamundaJavaApiService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.TaskFilterService;

import java.time.LocalDate;
import java.util.List;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith({MockitoExtension.class, ProcessEngineExtension.class})
@Deployment(resources = {"sample-test-process.bpmn"})
public class BaseSortingTest {

    /*This is sample process deployed to run the test cases. process file is present in the resources folder
     * Should file be changed then below process specific variables should be replaced with new uploaded process key */
    protected static final String PROCESS_KEY_VALUE = "sample_test_custom_task_process";
    protected static final Logger LOGGER = getLogger(BaseSortingTest.class);
    protected static final String IS_URGENT = "isUrgent";
    protected static final String HEARING_DATE = "hearingDate";
    protected static final String ORGANISATION_ID = "organisationId";

    protected final LocalDate now = LocalDate.now();

    @RegisterExtension
    ProcessEngineExtension extension = ProcessEngineExtension.builder()
            .configurationResource("camunda.cfg.xml")
            .build();
    protected ProcessInstance processInstance;
    protected CamundaJavaApiService camundaJavaApiService = new CamundaJavaApiService();
    protected TaskFilterService taskFilterService = new TaskFilterService();
    protected TaskService taskService;
    protected RuntimeService runtimeService;

    @BeforeEach
    public void setUp() throws Exception {
        taskService = extension.getProcessEngine().getTaskService();
        runtimeService = extension.getProcessEngine().getRuntimeService();
        setField(taskFilterService, "taskService", taskService);
        setField(camundaJavaApiService, "taskService", taskService);
        setField(camundaJavaApiService, "runtimeService", runtimeService);
        setField(camundaJavaApiService, "taskFilterService", taskFilterService);
    }

    protected List<Task> getTasks() {
        processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_VALUE, randomUUID().toString());
        final String processInstanceId = processInstance.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, ORGANISATION_ID, "e7be77a9-9ac5-431d-a23b-2d750e441b75");
        return taskService.createTaskQuery().processInstanceId(processInstanceId).active().orderByTaskName().asc().list();
    }

    @Test
    public void testDoNothing() {
        //Added as placeholder
    }
}
