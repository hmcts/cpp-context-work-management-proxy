package uk.gov.moj.cpp.workmanagement.proxy.api.permission;

import static uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission.builder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PermissionProvider {
    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private static final String UPDATE = "Update";
    private static final String VIEW = "View";
    private static final String CTSC_MANAGER = "CTSC_Manager";
    private static final String OBJECT = "WorkManagement";

    private PermissionProvider() {
    }

    public static String[] getViewPermission() throws JsonProcessingException {
        final ExpectedPermission viewPermission = builder()
                .withAction(VIEW)
                .withObject(OBJECT)
                .build();
        return new String[]{objectMapper.writeValueAsString(viewPermission)};
    }

    public static String[] getUpdatePermission() throws JsonProcessingException {
        final ExpectedPermission updatePermission = builder()
                .withAction(UPDATE)
                .withObject(OBJECT)
                .build();
        return new String[]{objectMapper.writeValueAsString(updatePermission)};
    }

    public static String[] getCTSCManagerPermission() throws JsonProcessingException {
        final ExpectedPermission managerPermission = builder()
                .withAction(VIEW)
                .withObject(CTSC_MANAGER)
                .build();
        return new String[]{objectMapper.writeValueAsString(managerPermission)};
    }
}
