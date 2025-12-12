package uk.gov.moj.cpp.workmanagement.proxy.api.interceptor;

import uk.gov.justice.services.core.accesscontrol.LocalAccessControlInterceptor;
import uk.gov.justice.services.core.audit.LocalAuditInterceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;
import uk.gov.justice.services.metrics.interceptor.IndividualActionMetricsInterceptor;
import uk.gov.justice.services.metrics.interceptor.TotalActionMetricsInterceptor;

import java.util.LinkedList;
import java.util.List;

public class WorkManagementProxyApiInterceptorChainProvider implements InterceptorChainEntryProvider {

    final List<InterceptorChainEntry> interceptorChainEntries = new LinkedList<>();

    public WorkManagementProxyApiInterceptorChainProvider() {
        interceptorChainEntries.add(new InterceptorChainEntry(1, TotalActionMetricsInterceptor.class));
        interceptorChainEntries.add(new InterceptorChainEntry(2, IndividualActionMetricsInterceptor.class));
        interceptorChainEntries.add(new InterceptorChainEntry(3000, LocalAuditInterceptor.class));
        interceptorChainEntries.add(new InterceptorChainEntry(4000, LocalAccessControlInterceptor.class));
        interceptorChainEntries.add(new InterceptorChainEntry(4001, ProxyRestApiInterceptor.class));
    }

    @Override
    public String component() {
        return "WorkManagement.Proxy.API";
    }

    public List<InterceptorChainEntry> interceptorChainTypes() {
        return interceptorChainEntries;
    }
}