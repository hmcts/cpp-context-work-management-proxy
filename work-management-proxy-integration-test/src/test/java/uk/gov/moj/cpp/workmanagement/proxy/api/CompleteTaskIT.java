package uk.gov.moj.cpp.workmanagement.proxy.api;

import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.workmanagement.proxy.api.helper.AbstractIT;
import uk.gov.moj.cpp.workmanagement.proxy.api.helper.WireMockStubUtils;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

/**
 * This integration test is only checking the negative scenario as the actual implementation is
 * validated as part of the command API unit test
 */
public class CompleteTaskIT extends AbstractIT {

    private static String DELETE_TASK = "/task/{0}";
    private String deletionReason = randomAlphabetic(20);

    @Test
    public void shouldThrowExceptionWhenDeletingNonExistentTask() {

        final String taskId = randomUUID().toString();
        final Response response = postCommand(format(getBaseUrl() + DELETE_TASK, taskId),
                "application/vnd.workmanagementproxy.complete-task+json",
                getDeletePayload(deletionReason)
        );
        assertThat(response.getStatus(), is(SC_BAD_REQUEST));
    }

    private String getDeletePayload(final String deletionReason) {
        return createObjectBuilder().add("deletionReason", deletionReason).build().toString();
    }
}
