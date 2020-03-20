package no.nav.k9.sak.behandlingskontroll.impl.observer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;

@ApplicationScoped
class BehandlingskontrollHenleggelseTransisjonEventObserver {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    BehandlingskontrollHenleggelseTransisjonEventObserver() {
        //for CDI proxy
    }

    @Inject
    public BehandlingskontrollHenleggelseTransisjonEventObserver(BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public void observerBehandlingSteg(@Observes BehandlingTransisjonEvent event) {

        if (FellesTransisjoner.HENLAGT.equals(event.getTransisjonIdentifikator())) {
            behandlingskontrollTjeneste.henleggBehandlingFraSteg(event.getKontekst(), BehandlingResultatType.HENLAGT_SØKNAD_MANGLER);

        }
    }
}



