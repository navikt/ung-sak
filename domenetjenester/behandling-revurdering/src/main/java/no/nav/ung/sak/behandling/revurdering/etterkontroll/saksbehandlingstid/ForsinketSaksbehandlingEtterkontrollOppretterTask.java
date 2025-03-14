package no.nav.ung.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.ung.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

@ApplicationScoped
@ProsessTask(value = ForsinketSaksbehandlingEtterkontrollOppretterTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class ForsinketSaksbehandlingEtterkontrollOppretterTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "forsinketsaksbehandling.etterkontroll.oppretter";
    private static final Logger log = LoggerFactory.getLogger(ForsinketSaksbehandlingEtterkontrollOppretterTask.class);

    private EtterkontrollRepository etterkontrollRepository;
    private BehandlingRepository behandlingRepository;
    private Period ventetidPeriode;

    private Instance<SaksbehandlingsfristUtleder> fristUtledere;

    public ForsinketSaksbehandlingEtterkontrollOppretterTask() {
    }

    @Inject
    public ForsinketSaksbehandlingEtterkontrollOppretterTask(
        EtterkontrollRepository etterkontrollRepository,
        @Any Instance<SaksbehandlingsfristUtleder> fristUtledere,
        BehandlingRepository behandlingRepository,
        @KonfigVerdi(value = "VENTETID_VED_UTGÅTT_SAKSBEHANDLINGSFRIST", defaultVerdi = "P0D") Period ventetidVedUtgåttFrist) {
        this.etterkontrollRepository = etterkontrollRepository;
        this.fristUtledere = fristUtledere;
        this.behandlingRepository = behandlingRepository;
        this.ventetidPeriode = ventetidVedUtgåttFrist;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fristUtleder = SaksbehandlingsfristUtleder.finnUtleder(behandling, fristUtledere);

        var fristOpt = fristUtleder.utledFrist(behandling);
        if (fristOpt.isEmpty()) {
            return;
        }

        LocalDateTime kontrollTidspunkt = bestemKontrolltidspunkt(fristOpt.get());

        log.info("Oppretter etterkontroll på tidspunkt {}.", kontrollTidspunkt);
        etterkontrollRepository.lagre(new Etterkontroll.Builder(behandling)
            .medKontrollTidspunkt(kontrollTidspunkt)
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .build()
        );

    }

    private LocalDateTime bestemKontrolltidspunkt(LocalDateTime frist) {
        var now = LocalDate.now();
        if (!frist.toLocalDate().isAfter(now)) {
            var utvidet = now.atStartOfDay().plus(ventetidPeriode);
            log.warn("Frist {} har allerede passert! Utvider kontrolltidspunkt med {} fra dagens dato til {}",
                frist, ventetidPeriode, utvidet);
            return utvidet;
        }

        return frist;
    }

}
