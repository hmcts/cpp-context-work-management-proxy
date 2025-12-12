package uk.gov.moj.cpp.workmanagement.proxy.ping.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class PingerClientDelegate {

    public CloseableHttpClient createDefaultClient() {
        return HttpClients.createDefault();
    }

    public HttpGet asHttpGet(final String url) {
        return new HttpGet(url);
    }

    public String asString(final HttpEntity httpEntity) throws IOException {
        return EntityUtils.toString(httpEntity);
    }
}
