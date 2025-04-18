package no.nav.ung.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;

@ApplicationScoped
public class ForsinketSaksbehandlingObserver {

    private static final Logger log = LoggerFactory.getLogger(ForsinketSaksbehandlingObserver.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;

    public ForsinketSaksbehandlingObserver() {
    }

    @Inject
    public ForsinketSaksbehandlingObserver(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void observerBehandlingOpprettet(@Observes BehandlingStatusEvent event) {
        if ((event.getGammelStatus() == BehandlingStatus.OPPRETTET || event.getGammelStatus() == null)
            && event.getNyStatus() == BehandlingStatus.UTREDES) {
            log.info("Vurderer behov for forsinket saksbehandling etterkontroll");
            var pd = ProsessTaskData.forProsessTask(ForsinketSaksbehandlingEtterkontrollOppretterTask.class);
            pd.setBehandling(event.getFagsakId(), event.getBehandlingId(), event.getAktørId().getAktørId());
            pd.setCallIdFraEksisterende();

            prosessTaskTjeneste.lagre(pd);
        }
    }
}
