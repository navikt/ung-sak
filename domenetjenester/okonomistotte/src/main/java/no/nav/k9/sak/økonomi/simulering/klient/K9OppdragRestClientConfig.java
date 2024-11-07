package no.nav.k9.sak.økonomi.simulering.klient;

import org.apache.http.impl.client.CloseableHttpClient;

import no.nav.k9.felles.integrasjon.rest.RestClientConfig;

public class K9OppdragRestClientConfig extends RestClientConfig {

    private static final int DEFAULT_MAX_PER_ROUTE = 20;
    private static final int MAX_TOTAL = 200;
    private static final int TIMEOUT_SEKUNDER = 60; //simuleringer med veldig mange perioder tar 20+ sekunder, så må øke fra default

    public K9OppdragRestClientConfig() {
        super(RestClientConfig.defaultRequestConfig().setSocketTimeout(TIMEOUT_SEKUNDER * 1000),
            MAX_TOTAL, DEFAULT_MAX_PER_ROUTE);
    }

    @Override
    public CloseableHttpClient createHttpClient() {
        return this.createHttpClientBuilder(getRequestConfig().build(), connectionManager(MAX_TOTAL, DEFAULT_MAX_PER_ROUTE), this.createKeepAliveStrategy(TIMEOUT_SEKUNDER), defaultHeaders()).build();
    }
}
