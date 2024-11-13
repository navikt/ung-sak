package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtSamling;
import no.nav.k9.felles.sikkerhet.abac.AbacSporingslogg;
import no.nav.k9.felles.sikkerhet.abac.PdpKlient;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.PdpRequestBuilder;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;

import java.util.Collections;

@Default
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 1)
public class AppPepImpl extends no.nav.k9.felles.sikkerhet.abac.PepImpl {

    private PdpRequestBuilder pdpRequestBuilder; //TODO fjern herfra, skal kun ligge i super-klassen

    AppPepImpl() {
    }

    @Inject
    public AppPepImpl(PdpKlient pdpKlient,
                      PdpRequestBuilder pdpRequestBuilder,
                      AbacSporingslogg sporingslogg,
                      @KonfigVerdi(value = "pip.users", required = false) String pipUsers,
                      @KonfigVerdi(value = "AZURE_APP_PRE_AUTHORIZED_APPS",required = false) String preAuthorized) {
        super(pdpKlient, pdpRequestBuilder, sporingslogg, pipUsers, preAuthorized);
        this.pdpRequestBuilder = pdpRequestBuilder;
    }

    @Override
    public Tilgangsbeslutning vurderTilgang(AbacAttributtSamling attributter) {
        // FIXME implementer tilgangskontoll
        if (ENV.isDev() || ENV.isLocal()){
            PdpRequest requestBrukesBareForSporingsloggHer = pdpRequestBuilder.lagPdpRequest(attributter);
            boolean fårTilgang = true;
            return new Tilgangsbeslutning(fårTilgang, Collections.emptyList(), requestBrukesBareForSporingsloggHer);
        } else {
            throw new RuntimeException("Ingen får tilgang inntil tilgangskontroll er implementert");
        }
    }

    /**
     * Ta hensyn til at flere aksjonspunker kan vurderes per request.
     */
    @Override
    protected int getAntallResources(PdpRequest pdpRequest) {
        return pdpRequest.getAntall(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE);
    }
}
