package uk.gov.moj.cpp.workmanagement.proxy.api.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtils() {
    }

    @SuppressWarnings("squid:S1166")
    public static boolean isJsonValid(String jsonInString) {
        try {
            mapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

