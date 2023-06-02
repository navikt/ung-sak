package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class ForsinketSaksbehandlingEtterkontrollOppretter {

    private EtterkontrollRepository etterkontrollRepository;
    private BehandlingRepository behandlingRepository;

    private Instance<SaksbehandlingsfristUtleder> fristUtledere;

    public ForsinketSaksbehandlingEtterkontrollOppretter() {
    }

    @Inject
    public ForsinketSaksbehandlingEtterkontrollOppretter(
        EtterkontrollRepository etterkontrollRepository,
        @Any Instance<SaksbehandlingsfristUtleder> fristUtledere,
        BehandlingRepository behandlingRepository) {
        this.etterkontrollRepository = etterkontrollRepository;
        this.fristUtledere = fristUtledere;
        this.behandlingRepository = behandlingRepository;
    }

    public void observerStartEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        SaksbehandlingsfristUtleder fristUtleder = finn(behandling);

        LocalDateTime frist = fristUtleder.utledFrist(behandling);

        etterkontrollRepository.lagre(new Etterkontroll.Builder(behandling)
            .medKontrollTidspunkt(frist)
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .build()
        );



    }

    private SaksbehandlingsfristUtleder finn(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(fristUtledere, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()));
    }
}
