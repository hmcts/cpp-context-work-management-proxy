package uk.gov.moj.cpp.workmanagement.proxy.api.rule;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.workmanagement.proxy.api.permission.PermissionProvider.getUpdatePermission;
import static uk.gov.moj.cpp.workmanagement.proxy.api.permission.PermissionProvider.getViewPermission;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class WorkflowRequestPermissionTest extends BaseDroolsAccessControlTest {
    private static Map<String, String[]> requestPermissionMap = new HashMap<>();

    static {
        try {
            requestPermissionMap.put("start-process", getUpdatePermission());
            requestPermissionMap.put("add-task-comment", getUpdatePermission());
            requestPermissionMap.put("complete-task", getUpdatePermission());
            requestPermissionMap.put("update-task-variable", getUpdatePermission());
            requestPermissionMap.put("get-task-list", getViewPermission());
            requestPermissionMap.put("get-task", getViewPermission());
            requestPermissionMap.put("reopen-task",  getUpdatePermission());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public WorkflowRequestPermissionTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowUserToSendUpdateAndViewRequest() {
        requestPermissionMap.forEach((actionName, permissions) -> {
            final Action action = createActionFor(actionName);
            when(userAndGroupProvider.hasPermission(action, permissions)).thenReturn(true);

            final ExecutionResults results = executeRulesWith(action);
            assertSuccessfulOutcome(results);
            verify(userAndGroupProvider).hasPermission(action, permissions);
            verifyNoMoreInteractions(userAndGroupProvider);
        });
    }

    @Test
    public void shouldNotAllowUserToSendUpdateAndViewRequest() {
        requestPermissionMap.forEach((actionName, permissions) -> {
            final Action action = createActionFor(actionName);
            when(userAndGroupProvider.hasPermission(action, permissions)).thenReturn(false);

            final ExecutionResults results = executeRulesWith(action);
            assertFailureOutcome(results);
            verify(userAndGroupProvider).hasPermission(action, permissions);
            verifyNoMoreInteractions(userAndGroupProvider);
        });
    }
}
