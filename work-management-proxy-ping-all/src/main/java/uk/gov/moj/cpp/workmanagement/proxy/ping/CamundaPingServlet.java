package uk.gov.moj.cpp.workmanagement.proxy.ping;

import static org.apache.http.HttpStatus.SC_OK;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(
        name = "camundaPingServlet",
        urlPatterns = CamundaPingServlet.BASE_ULR_PATH
)
public class CamundaPingServlet extends HttpServlet {

    @SuppressWarnings("squid:S1075")
    public static final String BASE_ULR_PATH = "/internal/healthchecks/camunda/ping/all";

    @Inject
    private CamundaContextPinger camundaContextPinger;

    @SuppressWarnings("squid:S1989")
    @Override
    protected void doGet(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws IOException {

        httpServletResponse.setContentType("text/plain; charset=UTF-8");
        httpServletResponse.setCharacterEncoding("UTF-8");

        final String responseMessage = camundaContextPinger.pingCamundaAndAllOtherCamundaContexts();

        httpServletResponse.setStatus(SC_OK);

        final PrintWriter out = httpServletResponse.getWriter();
        out.println(responseMessage);
        out.flush();
    }
}
