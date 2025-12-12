package uk.gov.moj.cpp.workmanagement.proxy.api.interceptor;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.interceptor.Interceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.workmanagement.proxy.api.exception.ExceptionProvider;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.RestClientService;
import uk.gov.moj.cpp.workmanagement.proxy.api.service.WorkManagementResponseBuilder;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;

public class ProxyRestApiInterceptor implements Interceptor {

    private static final Logger LOGGER = getLogger(ProxyRestApiInterceptor.class);
    private static final String GET = "get";
    private static final String DELETE = "delete";
    private static final String PUT = "put";
    private static final String POST = "post";

    // list of actions that should not be sent to camunda
    private static final List<String> whitelistedActionNames =
            newArrayList("complete-task", "custom-query-assignee-tasks-with-variables",
            "custom-query-available-tasks-with-variables", "update-task", "assign-task", "task-history", "create-custom-task",
            "create-generic-task", "activity-summary", "get-task-details", "reopen-task", "update-task-variable");

    @Inject
    HttpServletRequest request;

    /**
     * defaultValue mock-engine-rest allows for testing outbound calls from interceptor to rest
     * engine whilst also having real camunda rest engine deployed simultaneously
     */
    @Inject
    @Value(key = "camundaBaseUrl", defaultValue = "http://localhost:8080/mock-engine-rest")
    private String camundaBaseUrl;

    @Inject
    private RestClientService restClientService;

    @Inject
    private ExceptionProvider exceptionProvider;

    @Inject
    private WorkManagementResponseBuilder responseBuilder;

    @Override
    @SuppressWarnings("squid:S2250")
    public InterceptorContext process(final InterceptorContext interceptorContext, final InterceptorChain interceptorChain) {
        final String method = request.getMethod();
        final String actionName = interceptorContext.inputEnvelope().metadata().name();
        final String servletPath = request.getServletPath();
        final String queryString = request.getQueryString();
        final String requestUrl = request.getRequestURL().toString();
        final String requestPath = requestUrl.substring(requestUrl.indexOf(servletPath) + servletPath.length());

        if (!whitelistedActionNames.contains(actionName)) {
            Response response;
            if (POST.equalsIgnoreCase(method)) {
                final String requestBody = interceptorContext.inputEnvelope().payloadAsJsonObject().toString();
                response = restClientService.post(camundaBaseUrl + requestPath, requestBody);
            } else if (PUT.equalsIgnoreCase(method)) {
                final String requestBody = interceptorContext.inputEnvelope().payloadAsJsonObject().toString();
                response = restClientService.put(camundaBaseUrl + requestPath, requestBody);
            } else if (DELETE.equalsIgnoreCase(method)) {
                response = restClientService.delete(camundaBaseUrl + requestPath);
            } else if (GET.equalsIgnoreCase(method)) {
                response = restClientService.get(camundaBaseUrl + requestPath, queryString);
            } else {
                LOGGER.warn("Unsupported operation : {}", method);
                throw new BadRequestException(method + "not supported");
            }

            if (nonNull(response) && response.getStatus() >= HttpStatus.SC_OK && response.getStatus() <= HttpStatus.SC_NO_CONTENT) {
                final JsonValue responsePayload = responseBuilder.getResponsePayload(response);
                final JsonEnvelope envelope = envelopeFrom(interceptorContext.inputEnvelope().metadata(), responsePayload);
                return interceptorContext.copyWithOutput(envelope);
            }

            exceptionProvider.throwApplicationSpecificException(response);

        }

        return interceptorChain.processNext(interceptorContext);

    }
}