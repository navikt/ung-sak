package no.nav.ung.sak.hendelse.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.ytelse.kontroll.ManglendeKontrollperioderTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NavigableSet;
import java.util.stream.Collectors;

@ApplicationScoped
@ProsessTask(VurderOmVedtakPåvirkerAndreSakerTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOmVedtakPåvirkerAndreSakerTask implements ProsessTaskHandler {

    public static final String TASKNAME = "iverksetteVedtak.vurderRevurderingEtterVedtak";
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private static final Logger log = LoggerFactory.getLogger(VurderOmVedtakPåvirkerAndreSakerTask.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private VurderManglendeKontrollAvPeriode vurderManglendeKontrollAvPeriode;

    VurderOmVedtakPåvirkerAndreSakerTask() {
    }

    @Inject
    public VurderOmVedtakPåvirkerAndreSakerTask(BehandlingRepository behandlingRepository,
                                                FagsakRepository fagsakRepository,
                                                FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                                VurderManglendeKontrollAvPeriode vurderManglendeKontrollAvPeriode) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.vurderManglendeKontrollAvPeriode = vurderManglendeKontrollAvPeriode;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var vedtakHendelse = JsonObjectMapper.fromJson(prosessTaskData.getPayloadAsString(), Ytelse.class);
        var fagsakYtelseType = mapYtelse(vedtakHendelse);
        LOG_CONTEXT.add("ytelseType", fagsakYtelseType);
        LOG_CONTEXT.add("saksnummer", vedtakHendelse.getSaksnummer());

        var kandidaterTilRevurdering = vurderManglendeKontrollAvPeriode.utledSakerMedPerioderSomErKanVærePåvirket(vedtakHendelse);

        log.info("Etter '{}' vedtak på saksnummer='{}', skal følgende saker '{}' som skal revurderes som følge av vedtak.",
            fagsakYtelseType, vedtakHendelse.getSaksnummer(), kandidaterTilRevurdering);

        for (SakMedPeriode kandidat : kandidaterTilRevurdering) {
            var taskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
            taskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, kandidat.getBehandlingÅrsakType().getKode());
            taskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODER, utledPerioder(kandidat.getPerioder()));

            var fagsak = fagsakRepository.hentSakGittSaksnummer(kandidat.getSaksnummer(), false)
                .orElseThrow();
            var tilRevurdering = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
                .orElseThrow();
            taskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(),
                tilRevurdering.getAktørId().getId());

            fagsakProsessTaskRepository.lagreNyGruppe(taskData);
        }
    }

    private String utledPerioder(NavigableSet<DatoIntervallEntitet> perioder) {
        return perioder.stream().map(it -> it.getFomDato() + "/" + it.getTomDato())
            .collect(Collectors.joining("|"));
    }

    static FagsakYtelseType mapYtelse(Ytelse vedtak) {
        if (vedtak.getYtelse() == Ytelser.UNGDOMSYTELSE) {
            return FagsakYtelseType.UNGDOMSYTELSE;
        }
        throw new IllegalArgumentException("Kunne ikke håndtere vedtak for ytelse: " + vedtak.getYtelse());
    }
}
