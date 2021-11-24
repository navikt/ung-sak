package no.nav.k9.sak.ytelse.omsorgspenger.ytelse.overlapp;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.domene.vedtak.ekstern.OverlappendeYtelserTjeneste;

@ApplicationScoped
@ProsessTask(VurderOverlappendeYtelserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class VurderOverlappendeYtelserTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "iverksetteVedtak.vurderOverlappendeYtelser";
    private static final Logger logger = LoggerFactory.getLogger(VurderOverlappendeYtelserTask.class);
    private BehandlingRepository behandlingRepository;
    private EtterkontrollRepository etterkontrollRepository;

    private OverlappendeYtelserTjeneste overlappendeYtelserTjeneste;

    VurderOverlappendeYtelserTask() {
        // CDI
    }

    @Inject
    public VurderOverlappendeYtelserTask(BehandlingRepository behandlingRepository,
                                         EtterkontrollRepository etterkontrollRepository,
                                         OverlappendeYtelserTjeneste overlappendeYtelserTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.etterkontrollRepository = etterkontrollRepository;
        this.overlappendeYtelserTjeneste = overlappendeYtelserTjeneste;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var overlappendeYtelser = overlappendeYtelserTjeneste.finnOverlappendeYtelser(BehandlingReferanse.fra(behandling));
        var erIkkeMarkertFraFør = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(), KontrollType.OVERLAPPENDE_YTELSE).isEmpty();
        if (!overlappendeYtelser.isEmpty() && erIkkeMarkertFraFør) {
            String formattert = overlappendeYtelser.keySet().stream()
                .map(key -> key + "=" + overlappendeYtelser.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
            logger.info("Oppretter VKY-oppgave fordi behandling har overlappende ytelser '{}'", formattert);

            etterkontrollRepository.lagre(new Etterkontroll.Builder(prosessTaskData.getFagsakId())
                .medErBehandlet(true)
                .medKontrollTidspunkt(LocalDateTime.now())
                .medKontrollType(KontrollType.OVERLAPPENDE_YTELSE)
                .build());
        }
    }
}
