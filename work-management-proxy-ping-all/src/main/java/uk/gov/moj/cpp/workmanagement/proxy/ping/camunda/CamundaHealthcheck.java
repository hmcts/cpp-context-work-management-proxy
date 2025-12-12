package uk.gov.moj.cpp.workmanagement.proxy.ping.camunda;

import javax.inject.Inject;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.slf4j.Logger;

public class CamundaHealthcheck {

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private RuntimeService runtimeService;

    public void checkForActiveCamundaProcessInstance() {

        logger.debug("Calling healthcheck on Camunda");

        final ProcessInstanceQuery processInstanceQuery = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId("")
                .active();

        if(processInstanceQuery == null) {
            throw new CamundaProcessInstanceNotFoundException("Failed to find active Camunda process instance");
        }

        logger.debug("Camunda process instance is active");
    }
}
