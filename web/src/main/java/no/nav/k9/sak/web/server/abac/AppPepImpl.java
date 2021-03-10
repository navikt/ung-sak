package no.nav.k9.sak.web.server.abac;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.AbacSporingslogg;
import no.nav.k9.felles.sikkerhet.abac.PdpKlient;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.PdpRequestBuilder;

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
                      @KonfigVerdi(value = "pip.users", required = false) String pipUsers) {
        super(pdpKlient, pdpRequestBuilder, sporingslogg, pipUsers);
    }

    /**
     * Ta hensyn til at flere aksjonspunker kan vurderes per request.
     */
    @Override
    protected int getAntallResources(PdpRequest pdpRequest) {
        return pdpRequest.getAntall(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE);
    }
}
