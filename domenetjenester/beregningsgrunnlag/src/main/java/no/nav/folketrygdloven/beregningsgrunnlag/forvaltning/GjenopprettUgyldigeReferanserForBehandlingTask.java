package no.nav.folketrygdloven.beregningsgrunnlag.forvaltning;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierOgResettBeregningListeRequest;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;

/**
 * Task som kaller kalkulus og gjenoppretter grunnlag og lagrer aktive grunnlag på nye referanser
 * <p>
 */
@ApplicationScoped
@ProsessTask(GjenopprettUgyldigeReferanserForBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class GjenopprettUgyldigeReferanserForBehandlingTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "beregning.gjenopprettReferanserForBehandling";
    public static final String NESTE_BEHANDLING_ID = "nesteBehandlingId";


    private KalkulusRestKlient kalkulusSystemRestKlient;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private EntityManager entityManager;

    @Inject
    public GjenopprettUgyldigeReferanserForBehandlingTask(SystemUserOidcRestClient systemUserOidcRestClient,
                                                          @KonfigVerdi(value = "ftkalkulus.url") URI endpoint,
                                                          FagsakRepository fagsakRepository,
                                                          BehandlingRepository behandlingRepository,
                                                          BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                                          EntityManager entityManager) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.entityManager = entityManager;
        this.kalkulusSystemRestKlient = new KalkulusRestKlient(systemUserOidcRestClient, endpoint);
    }


    public GjenopprettUgyldigeReferanserForBehandlingTask() {
    }

    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        var fagsak = fagsakRepository.finnUnikFagsak(fagsakId).orElseThrow();
        var behandlingId = prosessTaskData.getBehandlingId();
        var nesteBehandlingId = prosessTaskData.getPropertyValue(NESTE_BEHANDLING_ID);
        var behandling1 = behandlingRepository.hentBehandling(behandlingId);
        var behandling2 = nesteBehandlingId == null ? null : behandlingRepository.hentBehandling(nesteBehandlingId);

        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(
            this.vilkårsPerioderTilVurderingTjeneste,
            fagsak.getYtelseType(),
            behandling1.getType());

        var ugyldigeReferanser = finnUgyldigeReferanserForBehandling(perioderTilVurderingTjeneste, behandling1);

        var originalBehandling = behandling1.getOriginalBehandlingId().map(behandlingRepository::hentBehandling).orElseThrow();

        var kopierBeregningRequests = lagNyeReferanser(ugyldigeReferanser);

        // Sett riktig initiell versjon i behandlingen som kommer etter i kronologisk rekkefølge
        if (behandling2 != null) {
            oppdaterInitiellVersjonForNesteBehandling(behandling2, kopierBeregningRequests);
        }

        oppdaterReferanseForAktivtGrunnlagIBehandling(behandling1, kopierBeregningRequests);

        // Gjenopprett i kalkulus
        gjenopprettIKalkulus(fagsak, behandling1, originalBehandling, kopierBeregningRequests);

    }

    private void oppdaterReferanseForAktivtGrunnlagIBehandling(Behandling behandling1, List<KopierBeregningRequest> kopierBeregningRequests) {
        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling1.getId());
        beregningsgrunnlagPerioderGrunnlag.stream()
            .flatMap(gr -> gr.getGrunnlagPerioder().stream())
            .forEach(p -> oppdaterReferanseForPeriodeHvisRelevant(kopierBeregningRequests, p));
    }

    private List<KopierBeregningRequest> lagNyeReferanser(Map<LocalDate, UUID> ugyldigeReferanser) {
        return ugyldigeReferanser.values().stream().map(r -> {
            var nyRef = new BgRef(null);
            return new KopierBeregningRequest(nyRef.getRef(), r);
        }).toList();
    }


    private void gjenopprettIKalkulus(Fagsak fagsak, Behandling nesteBehandling,
                                      Behandling originalBehandling,
                                      List<KopierBeregningRequest> kopierBeregningRequests) {
        var request = new KopierOgResettBeregningListeRequest(fagsak.getSaksnummer().getVerdi(),
            nesteBehandling.getUuid(),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(fagsak.getYtelseType().getKode()),
            StegType.FAST_BERGRUNN,
            kopierBeregningRequests,
            originalBehandling.getAvsluttetDato());
        kalkulusSystemRestKlient.kopierOgResettBeregning(request);
    }

    private Map<LocalDate, UUID> finnUgyldigeReferanserForBehandling(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, Behandling nesteBehandling) {
        var skjæringstidspunkterTilVurdering = finnSkjæringstidspunkterTilVurdering(nesteBehandling, perioderTilVurderingTjeneste);
        var vurderteEksternReferanserMap = finnVurderteReferanser(nesteBehandling.getId(), skjæringstidspunkterTilVurdering);
        var originaleVurderteReferanser = finnOriginaleVurderteReferanser(nesteBehandling, vurderteEksternReferanserMap);
        return finnLikeReferanser(vurderteEksternReferanserMap, originaleVurderteReferanser);
    }

    private void oppdaterInitiellVersjonForNesteBehandling(Behandling forrigeBehandling, List<KopierBeregningRequest> kopierBeregningRequests) {
        var grunnlagInitiellVersjonOpt = beregningPerioderGrunnlagRepository.getInitiellVersjon(forrigeBehandling.getId());
        if (grunnlagInitiellVersjonOpt.isPresent()) {
            var initiellVersjon = grunnlagInitiellVersjonOpt.get();
            initiellVersjon.getGrunnlagPerioder()
                .forEach(p -> oppdaterReferanseForPeriodeHvisRelevant(kopierBeregningRequests, p));
        }
    }

    private void oppdaterReferanseForPeriodeHvisRelevant(List<KopierBeregningRequest> kopierBeregningRequests, BeregningsgrunnlagPeriode p) {
        var nyReferanse = finnNyReferanse(kopierBeregningRequests, p);
        if (nyReferanse.isPresent()) {
            p.setEksternReferanse(nyReferanse.get());
            entityManager.persist(p);
        }
    }

    private Optional<UUID> finnNyReferanse(List<KopierBeregningRequest> kopierBeregningRequests, BeregningsgrunnlagPeriode p) {
        return kopierBeregningRequests.stream().filter(r -> r.getKopierFraReferanse().equals(p.getEksternReferanse())).map(KopierBeregningRequest::getEksternReferanse).findFirst();
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
