package uk.gov.moj.cpp.workmanagement.proxy.api.util;

import static com.google.common.io.Resources.getResource;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CamundaProxyApiTestBase {

    public static <T> T readJson(final String jsonPath, final Class<T> clazz) {
        try {
            final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

            return OBJECT_MAPPER.readValue(getResource(jsonPath), clazz);
        } catch (final IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible: " + e.getMessage());
        }
    }
}
