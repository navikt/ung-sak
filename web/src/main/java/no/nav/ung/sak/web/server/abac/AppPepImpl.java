package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.AbacSporingslogg;
import no.nav.k9.felles.sikkerhet.abac.PdpKlient;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.PdpRequestBuilder;
import no.nav.ung.sak.tilgangskontroll.api.AbacAttributter;

@Default
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 1)
public class AppPepImpl extends no.nav.k9.felles.sikkerhet.abac.PepImpl {

    AppPepImpl() {
    }

    @Inject
    public AppPepImpl(PdpKlient pdpKlient,
                      PdpRequestBuilder pdpRequestBuilder,
                      AbacSporingslogg sporingslogg,
                      @KonfigVerdi(value = "pip.users", required = false) String pipUsers,
                      @KonfigVerdi(value = "AZURE_APP_PRE_AUTHORIZED_APPS",required = false) String preAuthorized) {
        super(pdpKlient, pdpRequestBuilder, sporingslogg, pipUsers, preAuthorized);
    }

    /**
     * Ta hensyn til at flere aksjonspunker kan vurderes per request.
     */
    @Override
    protected int getAntallResources(PdpRequest pdpRequest) {
        return pdpRequest.getAntall(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE);
    }
}
