package uk.gov.moj.cpp.workmanagement.proxy.api;

import static java.text.MessageFormat.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.workmanagement.proxy.api.helper.AbstractIT;
import uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkManagementProcessDefinitionIT extends AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkManagementProcessDefinitionIT.class);
    private static final String PROCESS_DEFINITION_START = "/process-definition/key/%s/start";
    private static final String KEY = "test_send_email_process";

    @Test
    public void shouldStartProcessInstanceByKey() {
        LOGGER.info("BASE_URI: {}, restClient {}", BASE_URI, restClient);
        final Response response = postCommand(format(getBaseUrl() + String.format(PROCESS_DEFINITION_START, KEY)),
                "application/vnd.workmanagementproxy.start-process+json",
                WireMockStubUtils.getPayload("stub-data/camunda-start-process-request-body.json"));
        assertThat(response.getStatus(), is(202));
    }

    @Test
    public void shouldThrowExceptionWhenStartingProcessInstanceByInvalidKey() {
        final Response response = postCommand(format(getBaseUrl() + String.format(PROCESS_DEFINITION_START, "test_send_email_process_invalid")),
                "application/vnd.workmanagementproxy.start-process+json",
                WireMockStubUtils.getPayload("stub-data/camunda-start-process-request-body.json"));
        assertThat(response.getStatus(), is(400)); //TODO need to check which HTTP code to be sent
    }
}