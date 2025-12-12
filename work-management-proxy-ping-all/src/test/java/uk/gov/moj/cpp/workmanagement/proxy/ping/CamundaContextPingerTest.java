package uk.gov.moj.cpp.workmanagement.proxy.ping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.workmanagement.proxy.ping.camunda.CamundaHealthcheck;
import uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.ContextPingLocation;
import uk.gov.moj.cpp.workmanagement.proxy.ping.configuration.ContextPingLocationProvider;
import uk.gov.moj.cpp.workmanagement.proxy.ping.http.ContextHttpPingException;
import uk.gov.moj.cpp.workmanagement.proxy.ping.http.HttpContextPingerClient;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class CamundaContextPingerTest {

    @Mock
    private CamundaHealthcheck camundaHealthcheck;

    @Mock
    private ContextPingLocationProvider contextPingLocationProvider;

    @Mock
    private HttpContextPingerClient httpContextPingerClient;

    @Mock
    private Logger logger;

    @InjectMocks
    private CamundaContextPinger camundaContextPinger;

    @BeforeEach
    void setUp() {
        when(httpContextPingerClient.ping(any(ContextPingLocation.class))).thenReturn("pong");
    }

    @Test
    void shouldPingCamundaAndAllOtherCamundaContextsSuccessfully() {
        ContextPingLocation location1 = mock(ContextPingLocation.class);
        ContextPingLocation location2 = mock(ContextPingLocation.class);
        when(location1.getContextName()).thenReturn("context1");
        when(location2.getContextName()).thenReturn("context2");
        when(contextPingLocationProvider.getContextPingLocations()).thenReturn(Arrays.asList(location1, location2));

        String result = camundaContextPinger.pingCamundaAndAllOtherCamundaContexts();

        verify(camundaHealthcheck, times(1)).checkForActiveCamundaProcessInstance();
        verify(contextPingLocationProvider, times(1)).getContextPingLocations();
        verify(httpContextPingerClient, times(1)).ping(location1);
        verify(httpContextPingerClient, times(1)).ping(location2);
        verify(logger, times(4)).debug(anyString());
        assertEquals("pong", result);
    }

    @Test
    void shouldThrowExceptionWhenPingResponseIsNotPong() {
        ContextPingLocation location = mock(ContextPingLocation.class);
        when(location.getContextName()).thenReturn("context1");
        when(contextPingLocationProvider.getContextPingLocations()).thenReturn(Collections.singletonList(location));
        when(httpContextPingerClient.ping(location)).thenReturn("error");

        assertThrows(ContextHttpPingException.class, () -> camundaContextPinger.pingCamundaAndAllOtherCamundaContexts());
        verify(logger, times(1)).debug(anyString());
    }
}
