package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.utils.Hjelpetidslinjer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.HentEtablertTilsynTjeneste;

@ApplicationScoped
@ProsessTask(VurderOmEtablertTilsynErEndretTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOmEtablertTilsynErEndretTask implements ProsessTaskHandler {

    public static final String TASKNAME = "drift.vurderOmEtablertTilsynErEndret";

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private HentEtablertTilsynTjeneste hentEtablertTilsynTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    

    VurderOmEtablertTilsynErEndretTask() {
    }

    @Inject
    public VurderOmEtablertTilsynErEndretTask(BehandlingRepository behandlingRepository,
                                                FagsakRepository fagsakRepository,
                                                FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                                HentEtablertTilsynTjeneste hentEtablertTilsynTjeneste,
                                                @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.hentEtablertTilsynTjeneste = hentEtablertTilsynTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final Saksnummer saksnummer = new Saksnummer(prosessTaskData.getSaksnummer());

        final var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer, false).orElseThrow();
        final var sisteBehandlingPåKandidat = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
                
        final NavigableSet<DatoIntervallEntitet> perioder = finnPerioderDerEtablertTilsynHarBlittEndret(sisteBehandlingPåKandidat);
        if (perioder.isEmpty()) {
            return;
        }
        
        final var taskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        taskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON.getKode());
        taskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODER, utledPerioder(perioder));

        taskData.setBehandling(sisteBehandlingPåKandidat.getFagsakId(), sisteBehandlingPåKandidat.getId(), sisteBehandlingPåKandidat.getAktørId().getId());

        fagsakProsessTaskRepository.lagreNyGruppe(taskData);
    }
    
    public NavigableSet<DatoIntervallEntitet> finnPerioderDerEtablertTilsynHarBlittEndret(Behandling sisteBehandlingPåKandidat) {
        final BehandlingReferanse ref = BehandlingReferanse.fra(sisteBehandlingPåKandidat);
        final LocalDateTimeline<Boolean> endringerMellomSmurtOgUsmurt = hentEtablertTilsynTjeneste.finnEndringerMellomSmurtOgUsmurt(ref);
        if (endringerMellomSmurtOgUsmurt.isEmpty()) {
            return Collections.emptyNavigableSet();
        }
        
        final var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, sisteBehandlingPåKandidat.getFagsakYtelseType(), sisteBehandlingPåKandidat.getType());
        final LocalDateTimeline<Boolean> perioderSkalRevurdereYtelse = tettHull(perioderTilVurderingTjeneste, endringerMellomSmurtOgUsmurt);
        
        return TidslinjeUtil.tilDatoIntervallEntiteter(perioderSkalRevurdereYtelse);
    }

    private LocalDateTimeline<Boolean> tettHull(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, LocalDateTimeline<Boolean> skalRevurderes) {
        var kantIKantVurderer = perioderTilVurderingTjeneste.getKantIKantVurderer();
        var tidslinjeHull = Hjelpetidslinjer.utledHullSomMåTettes(skalRevurderes, kantIKantVurderer);
        return skalRevurderes.crossJoin(tidslinjeHull, StandardCombinators::coalesceRightHandSide);
    }

    private String utledPerioder(NavigableSet<DatoIntervallEntitet> perioder) {
        return perioder.stream().map(it -> it.getFomDato() + "/" + it.getTomDato())
            .collect(Collectors.joining("|"));
    }
}
