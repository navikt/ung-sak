package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

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
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

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
        @KonfigVerdi(value = "VENTETID_VED_UTGÅTT_SAKSBEHANDLINGSFRIST", required = false) String ventetidVedUtgåttFrist) {
        this.etterkontrollRepository = etterkontrollRepository;
        this.fristUtledere = fristUtledere;
        this.behandlingRepository = behandlingRepository;
        this.ventetidPeriode = ventetidVedUtgåttFrist != null ? Period.parse(ventetidVedUtgåttFrist) : null;
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

        LocalDateTime frist = bestemFaktiskFrist(fristOpt.get());

        log.info("Oppretter etterkontroll med frist {}", frist);
        etterkontrollRepository.lagre(new Etterkontroll.Builder(behandling)
            .medKontrollTidspunkt(frist)
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .build()
        );

    }

    private LocalDateTime bestemFaktiskFrist(LocalDateTime utledetFrist) {
        var now = LocalDate.now();
        if (ventetidPeriode != null && !utledetFrist.toLocalDate().isAfter(now)) {
            log.info("Frist {} er på eller før dagens dato. Utvider frist med {} fra dagens dato", utledetFrist, ventetidPeriode);
            return now.atStartOfDay().plus(ventetidPeriode);
        }

        return utledetFrist;
    }

}
