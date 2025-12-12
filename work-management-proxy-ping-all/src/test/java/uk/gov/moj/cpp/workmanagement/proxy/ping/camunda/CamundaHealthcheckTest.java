package uk.gov.moj.cpp.workmanagement.proxy.ping.camunda;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class CamundaHealthcheckTest {

    @Mock
    private Logger logger;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ProcessInstanceQuery processInstanceQuery;

    @InjectMocks
    private CamundaHealthcheck camundaHealthcheck;

    @BeforeEach
    void setUp() {
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(anyString())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.active()).thenReturn(processInstanceQuery);
    }

    @Test
    void shouldCheckForActiveCamundaProcessInstance() {
        camundaHealthcheck.checkForActiveCamundaProcessInstance();

        verify(logger).debug("Calling healthcheck on Camunda");
        verify(runtimeService).createProcessInstanceQuery();
        verify(processInstanceQuery).processInstanceId("");
        verify(processInstanceQuery).active();
        verify(logger).debug("Camunda process instance is active");
    }

    @Test
    void shouldThrowExceptionWhenProcessInstanceQueryIsNull() {
        when(processInstanceQuery.active()).thenReturn(null);

        assertThrows(CamundaProcessInstanceNotFoundException.class, () -> camundaHealthcheck.checkForActiveCamundaProcessInstance());
        verify(logger).debug("Calling healthcheck on Camunda");
    }
}
