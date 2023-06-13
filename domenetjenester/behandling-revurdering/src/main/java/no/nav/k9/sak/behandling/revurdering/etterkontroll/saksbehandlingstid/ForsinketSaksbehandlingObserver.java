package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;

@ApplicationScoped
public class ForsinketSaksbehandlingObserver {

    private static final Logger log = LoggerFactory.getLogger(ForsinketSaksbehandlingObserver.class);

    private boolean isEnabled;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public ForsinketSaksbehandlingObserver() {}

    @Inject
    public ForsinketSaksbehandlingObserver(
        @KonfigVerdi(value = "ENABLE_ETTERKONTROLL_FORSINKET_SAKB", defaultVerdi = "false") boolean isEnabled,
        ProsessTaskTjeneste prosessTaskTjeneste) {
        this.isEnabled = isEnabled;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void observerBehandlingOpprettet(@Observes BehandlingStatusEvent event) {
        if (isEnabled && event.getNyStatus() == BehandlingStatus.UTREDES) {
            log.info("Vurderer behov for forsinket saksbehandling etterkontroll");
            var pd = ProsessTaskData.forProsessTask(ForsinketSaksbehandlingEtterkontrollOppretterTask.class);
            pd.setBehandling(event.getFagsakId(), event.getBehandlingId(), event.getAktørId().getAktørId());
            pd.setCallIdFraEksisterende();

            prosessTaskTjeneste.lagre(pd);
        }
    }
}
