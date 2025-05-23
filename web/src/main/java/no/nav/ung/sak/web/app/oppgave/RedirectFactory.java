package no.nav.ung.sak.web.app.oppgave;

import jakarta.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.ContextPathHolder;

class RedirectFactory {

    private String loadBalancerUrl;

    //TODO (TOR) Denne bør ikkje ligga på server, men heller automatisk redirecte i klient. (Men dette er tryggast løysing tett opptil release)
    private String defaultPartUrl = "?punkt=default&fakta=default";

    String lagRedirect(OppgaveRedirectData data) {
        if (data.harFeilmelding()) {
            return String.format("%s/#?errormessage=%s", getBaseUrl(), data.getFeilmelding()); //$NON-NLS-1$
        }
        if (data.harBehandlingId()) {
            return String.format("%s/fagsak/%s/behandling/%d/%s", getBaseUrl(), data.getSaksnummer().getVerdi(), data.getBehandlingId(), defaultPartUrl); //$NON-NLS-1$
        }
        return String.format("%s/fagsak/%s/", getBaseUrl(), data.getSaksnummer().getVerdi());//$NON-NLS-1$
    }

    protected String getBaseUrl() {
        return loadBalancerUrl + ContextPathHolder.instance().getContextPath();
    }

    @Inject
    public void setLoadBalancerUrl(@KonfigVerdi("loadbalancer.url") String loadBalancerUrl) {
        this.loadBalancerUrl = loadBalancerUrl;
    }
}
