package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@Dependent
@Priority(2) //For å kunne override i test
public class ForsinketSaksbehandlingEtterkontrollOppretterImpl implements ForsinketSaksbehandlingEtterkontrollOppretter {

    private static final Logger log = LoggerFactory.getLogger(ForsinketSaksbehandlingEtterkontrollOppretterImpl.class);

    private EtterkontrollRepository etterkontrollRepository;
    private BehandlingRepository behandlingRepository;

    private Instance<SaksbehandlingsfristUtleder> fristUtledere;

    public ForsinketSaksbehandlingEtterkontrollOppretterImpl() {
    }

    @Inject
    public ForsinketSaksbehandlingEtterkontrollOppretterImpl(
        EtterkontrollRepository etterkontrollRepository,
        @Any Instance<SaksbehandlingsfristUtleder> fristUtledere,
        BehandlingRepository behandlingRepository) {
        this.etterkontrollRepository = etterkontrollRepository;
        this.fristUtledere = fristUtledere;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void opprettEtterkontroll(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fristUtleder = finnUtleder(behandling);

        if(fristUtleder.isEmpty()) {
            return;
        }

        LocalDateTime frist = fristUtleder.get().utledFrist(behandling);
        log.info("Oppretter etterkontroll med frist {}", frist);

        etterkontrollRepository.lagre(new Etterkontroll.Builder(behandling)
            .medKontrollTidspunkt(frist)
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .build()
        );

    }

    private Optional<SaksbehandlingsfristUtleder> finnUtleder(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(fristUtledere, behandling.getFagsakYtelseType());
    }
}
