package uk.gov.moj.cpp.workmanagement.proxy.ping;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CamundaPingServletTest {

    @Mock
    private CamundaContextPinger camundaContextPinger;
    
    @InjectMocks
    private CamundaPingServlet camundaPingServlet;

    @Test
    public void shouldPingCamundaAndAllOtherCamundaContexts() throws Exception {

        final String responseMessage = "the ping response string";

        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final PrintWriter out = mock(PrintWriter.class);

        when(camundaContextPinger.pingCamundaAndAllOtherCamundaContexts()).thenReturn(responseMessage);
        when(httpServletResponse.getWriter()).thenReturn(out);

        camundaPingServlet.doGet(httpServletRequest, httpServletResponse);

        final InOrder inOrder = inOrder(httpServletResponse, out);

        inOrder.verify(httpServletResponse).setContentType("text/plain; charset=UTF-8");
        inOrder.verify(httpServletResponse).setCharacterEncoding("UTF-8");
        inOrder.verify(out).println(responseMessage);
        inOrder.verify(out).flush();
    }
}