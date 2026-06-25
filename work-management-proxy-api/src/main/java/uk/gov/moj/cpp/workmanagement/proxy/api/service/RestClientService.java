package uk.gov.moj.cpp.workmanagement.proxy.api.service;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import uk.gov.justice.services.common.configuration.Value;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.parseInt;

@ApplicationScoped
public class RestClientService {

    @Inject
    @Value(key = "restClient.httpConnection.poolSize", defaultValue = "10")
    private String httpConnectionPoolSize;

    @Inject
    @Value(key = "restClient.httpConnection.timeout", defaultValue = "120000")//2minutes
    private String httpConnectionTimeout;

    private Client client;

    @PostConstruct
    public void init() {
        final int poolSize = parseInt(httpConnectionPoolSize);
        final int timeout = parseInt(httpConnectionTimeout);

        client = new ResteasyClientBuilderImpl()
                .connectionPoolSize(poolSize)
                .maxPooledPerRoute(poolSize)
                .connectionTTL(timeout, TimeUnit.MILLISECONDS)
                .connectionCheckoutTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
    }


    @PreDestroy
    public void preDestroy() {
        client.close();
    }

    public Response get(final String url, final String queryString) {
        final WebTarget target = client.target(url + "?" + queryString);

        /**
         * Easier to the above appending query string rather than constructing query params
         *
         * approach below needs to be relooked
         */
        return target
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
    }


    public Response post(final String url, final String requestBody) {
        final Entity<String> entity = Entity.entity(requestBody, MediaType.APPLICATION_JSON);
        return client
                .target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(entity);
    }

    public Response put(final String url, final String requestBody) {
        final Entity<String> entity = Entity.entity(requestBody, MediaType.APPLICATION_JSON);
        return client
                .target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .put(entity);
    }

    public Response delete(final String url) {
        return client
                .target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();
    }
}
