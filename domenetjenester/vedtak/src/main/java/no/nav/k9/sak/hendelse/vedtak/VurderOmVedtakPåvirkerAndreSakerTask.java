package no.nav.k9.sak.hendelse.vedtak;

import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;

@ApplicationScoped
@ProsessTask(VurderOmVedtakPåvirkerAndreSakerTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOmVedtakPåvirkerAndreSakerTask implements ProsessTaskHandler {

    public static final String TASKNAME = "iverksetteVedtak.vurderRevurderingAndreSøknader";
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private static final Logger log = LoggerFactory.getLogger(VurderOmVedtakPåvirkerAndreSakerTask.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    VurderOmVedtakPåvirkerAndreSakerTask() {
    }

    @Inject
    public VurderOmVedtakPåvirkerAndreSakerTask(BehandlingRepository behandlingRepository,
                                                FagsakRepository fagsakRepository,
                                                FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var vedtakHendelse = JsonObjectMapper.fromJson(prosessTaskData.getPayloadAsString(), Ytelse.class);
        var fagsakYtelseType = FagsakYtelseType.fromString(vedtakHendelse.getType().getKode());
        LOG_CONTEXT.add("ytelseType", fagsakYtelseType);

        var vurderOmVedtakPåvirkerSakerTjeneste = VurderOmVedtakPåvirkerSakerTjeneste.finnTjeneste(fagsakYtelseType);
        var kandidaterTilRevurdering = vurderOmVedtakPåvirkerSakerTjeneste.utledSakerMedPerioderSomErKanVærePåvirket(vedtakHendelse);

        log.info("Etter '{}' vedtak på saksnummer='{}', skal følgende saker '{}' som skal revurderes som følge av vedtak.", fagsakYtelseType, vedtakHendelse.getSaksnummer(), kandidaterTilRevurdering);

        for (SakMedPeriode kandidatsaksnummer : kandidaterTilRevurdering) {
            ProsessTaskData tilRevurderingTaskData = new ProsessTaskData(OpprettRevurderingEllerOpprettDiffTask.TASKNAME);
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON.getKode());
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODER, utledPerioder(kandidatsaksnummer.getPerioder()));
            var fagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer.getSaksnummer(), false).orElseThrow();
            var tilRevurdering = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
            tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());

            fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
        }
    }

    private String utledPerioder(NavigableSet<DatoIntervallEntitet> perioder) {
        return perioder.stream().map(it -> it.getFomDato() + "/" + it.getTomDato()).collect(Collectors.joining("|"));
    }
}
