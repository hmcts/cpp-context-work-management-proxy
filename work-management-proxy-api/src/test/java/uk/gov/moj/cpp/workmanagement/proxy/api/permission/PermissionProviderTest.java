package uk.gov.moj.cpp.workmanagement.proxy.api.permission;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class PermissionProviderTest {

    private static final String VIEW="View";

    @Test
    void shouldGetViewPermission() throws JsonProcessingException {
        final String[] test = PermissionProvider.getViewPermission();
        final String json = Arrays.stream(test).toList().get(0);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode = mapper.readTree(json);
        assertThat(rootNode.get("action").asText(), is(VIEW));
    }

    @Test
    void shouldUpdatePermission() throws JsonProcessingException {
        final String[] test = PermissionProvider.getUpdatePermission();
        final String json = Arrays.stream(test).toList().get(0);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode = mapper.readTree(json);
        assertThat(rootNode.get("action").asText(), is("Update"));
    }

    @Test
    void shouldGetCTSCManagerPermission() throws JsonProcessingException {
        final String[] test = PermissionProvider.getCTSCManagerPermission();
        final String json = Arrays.stream(test).toList().get(0);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode = mapper.readTree(json);
        assertThat(rootNode.get("action").asText(), is(VIEW));
        assertThat(rootNode.get("object").asText(), is("CTSC_Manager"));
    }
}
