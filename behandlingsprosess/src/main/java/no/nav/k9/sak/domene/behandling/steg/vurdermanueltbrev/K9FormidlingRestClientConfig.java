package no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev;

import no.nav.k9.felles.integrasjon.rest.RestClientConfig;
import org.apache.http.impl.client.CloseableHttpClient;

public class K9FormidlingRestClientConfig extends RestClientConfig {

    private static final int DEFAULT_MAX_PER_ROUTE = 20;
    private static final int MAX_TOTAL = 200;
    private static final int TIMEOUT_SEKUNDER = 60; //kall til formidling lager kall tilbake til k9sak (vilkaar-v3 kan ta 12sek og brukes både i dokumentdata og formidling)

    public K9FormidlingRestClientConfig() {
        super(RestClientConfig.defaultRequestConfig().setSocketTimeout(TIMEOUT_SEKUNDER * 1000),
            MAX_TOTAL, DEFAULT_MAX_PER_ROUTE);
    }

    @Override
    public CloseableHttpClient createHttpClient() {
        return this.createHttpClientBuilder(getRequestConfig().build(), connectionManager(MAX_TOTAL, DEFAULT_MAX_PER_ROUTE), this.createKeepAliveStrategy(TIMEOUT_SEKUNDER), defaultHeaders()).build();
    }
}
