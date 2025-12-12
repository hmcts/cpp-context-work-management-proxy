package uk.gov.moj.cpp.workmanagement.proxy.ping.configuration;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ContextPingLocation {

    private final String contextName;
    private final String hostName;
    private final int port;

    public ContextPingLocation(final String contextName, final String hostName, final int port) {
        this.contextName = contextName;
        this.hostName = hostName;
        this.port = port;
    }

    public String getContextName() {
        return contextName;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    @Override
    @SuppressWarnings({"squid:S00121", "squid:S00122"})
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (!(o instanceof ContextPingLocation)) return false;

        final ContextPingLocation that = (ContextPingLocation) o;

        return new EqualsBuilder().append(port, that.port).append(contextName, that.contextName).append(hostName, that.hostName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(contextName).append(hostName).append(port).toHashCode();
    }
}
