package no.nav.ung.sak.behandlingskontroll.impl.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;

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
            behandlingskontrollTjeneste.henleggBehandlingFraSteg(event.getKontekst(), BehandlingResultatType.HENLAGT_MASKINELT);
        }
    }
}



