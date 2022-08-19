package no.nav.folketrygdloven.beregningsgrunnlag.forvaltning;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;

/**
 * Task som kaller kalkulus og gjenoppretter grunnlag og lagrer aktive grunnlag på nye referanser
 * <p>
 */
@ApplicationScoped
@ProsessTask(GjenopprettUgyldigeReferanserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class GjenopprettUgyldigeReferanserTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "beregning.gjenopprettReferanser";


    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public GjenopprettUgyldigeReferanserTask(FagsakRepository fagsakRepository,
                                             BehandlingRepository behandlingRepository,
                                             BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                             ProsessTaskTjeneste prosessTaskTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    public GjenopprettUgyldigeReferanserTask() {
    }

    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = prosessTaskData.getSaksnummer();
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        var sisteBehandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(
            this.vilkårsPerioderTilVurderingTjeneste,
            fagsak.getYtelseType(),
            sisteBehandling.getType());

        var nesteBehandling = finnStartBehandling(sisteBehandling, perioderTilVurderingTjeneste);
        var ugyldigeReferanser = finnUgyldigeReferanserForBehandling(perioderTilVurderingTjeneste, nesteBehandling);
        Behandling forrigeBehandling = null;

        // Looper i omvendt kronologisk rekkefølge. Nøkkelordene "forrige" og "neste" refererer til iterasjonens rekkefølge og ikke kronologisk rekkefølge
        while (!ugyldigeReferanser.isEmpty()) {

            var gjenopprettTaskData = ProsessTaskData.forProsessTask(GjenopprettUgyldigeReferanserForBehandlingTask.class);
            gjenopprettTaskData.setFagsakId(fagsak.getId());
            gjenopprettTaskData.setBehandling(fagsak.getId(), nesteBehandling.getId());
            // setter neste behandling id i task til forrigebehandlingId siden vi looper omvendt kronologisk her
            gjenopprettTaskData.setProperty(GjenopprettUgyldigeReferanserForBehandlingTask.NESTE_BEHANDLING_ID, forrigeBehandling == null ? null : String.valueOf(forrigeBehandling.getId()));
            prosessTaskTjeneste.lagre(gjenopprettTaskData);

            // Oppdaterer for neste iterasjon
            forrigeBehandling = nesteBehandling;
            nesteBehandling = nesteBehandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling).orElseThrow();
            ugyldigeReferanser = finnUgyldigeReferanserForBehandling(perioderTilVurderingTjeneste, nesteBehandling);
        }

    }

    private Behandling finnStartBehandling(Behandling sisteBehandling, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var startBehandling = sisteBehandling;
        var startUgyldigeReferanser = finnUgyldigeReferanserForBehandling(perioderTilVurderingTjeneste, startBehandling);

        while (startUgyldigeReferanser.isEmpty() && startBehandling.getOriginalBehandlingId().isPresent()) {
            startBehandling = behandlingRepository.hentBehandling(startBehandling.getOriginalBehandlingId().orElseThrow());
            startUgyldigeReferanser = finnUgyldigeReferanserForBehandling(perioderTilVurderingTjeneste, startBehandling);
        }

        if (startUgyldigeReferanser.isEmpty()) {
            throw new IllegalStateException("Forventet å finne ugyldige referanser for sak");
        }

        return startBehandling;
    }


    private Map<LocalDate, UUID> finnUgyldigeReferanserForBehandling(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, Behandling nesteBehandling) {
        var skjæringstidspunkterTilVurdering = finnSkjæringstidspunkterTilVurdering(nesteBehandling, perioderTilVurderingTjeneste);
        var vurderteEksternReferanserMap = finnVurderteReferanser(nesteBehandling.getId(), skjæringstidspunkterTilVurdering);
        var originaleVurderteReferanser = finnOriginaleVurderteReferanser(nesteBehandling, vurderteEksternReferanserMap);
        return finnLikeReferanser(vurderteEksternReferanserMap, originaleVurderteReferanser);
    }


    private Map<LocalDate, UUID> finnOriginaleVurderteReferanser(Behandling nesteBehandling, Map<LocalDate, UUID> vurderteEksternReferanserMap) {
        if (nesteBehandling.getOriginalBehandlingId().isPresent()) {
            return finnVurderteReferanser(nesteBehandling.getOriginalBehandlingId().get(), vurderteEksternReferanserMap.keySet());
        }
        return Map.of();
    }


    private Set<LocalDate> finnSkjæringstidspunkterTilVurdering(Behandling sisteBehandling, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var tilVurdering = perioderTilVurderingTjeneste.utled(sisteBehandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return tilVurdering.stream().map(DatoIntervallEntitet::getFomDato).collect(Collectors.toSet());
    }

    private Map<LocalDate, UUID> finnLikeReferanser(Map<LocalDate, UUID> vurderteEksternReferanser, Map<LocalDate, UUID> originaleVurderteReferanser) {
        return vurderteEksternReferanser.entrySet().stream()
            .filter(e -> originaleVurderteReferanser.containsValue(e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<LocalDate, UUID> finnVurderteReferanser(Long behandlingId, Set<LocalDate> skjæringstidspunkterTilVurdering) {
        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandlingId);
        return beregningsgrunnlagPerioderGrunnlag.stream().flatMap(b -> b.getGrunnlagPerioder().stream())
            .filter(g -> skjæringstidspunkterTilVurdering.stream().anyMatch(g.getSkjæringstidspunkt()::equals))
            .collect(Collectors.toMap(BeregningsgrunnlagPeriode::getSkjæringstidspunkt, BeregningsgrunnlagPeriode::getEksternReferanse));
    }


}
