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
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
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


    private KalkulusRestKlient kalkulusSystemRestKlient;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private EntityManager entityManager;

    @Inject
    public GjenopprettUgyldigeReferanserTask(SystemUserOidcRestClient systemUserOidcRestClient,
                                             @KonfigVerdi(value = "ftkalkulus.url") URI endpoint,
                                             FagsakRepository fagsakRepository,
                                             BehandlingRepository behandlingRepository,
                                             BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                             Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                             EntityManager entityManager) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.entityManager = entityManager;
        this.kalkulusSystemRestKlient = new KalkulusRestKlient(systemUserOidcRestClient, endpoint);
    }


    public GjenopprettUgyldigeReferanserTask() {
    }

    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = prosessTaskData.getSaksnummer();
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        var sisteBehandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        var perioderTilVurderingTjeneste = FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral.Lookup.find(this.vilkårsPerioderTilVurderingTjeneste, fagsak.getYtelseType()).orElseThrow();

        var nesteBehandling = sisteBehandling;
        var originalBehandling = behandlingRepository.hentBehandling(nesteBehandling.getOriginalBehandlingId().orElseThrow());
        var ugyldigeReferanser = finnUgyldigeReferanserForBehandling(perioderTilVurderingTjeneste, nesteBehandling);
        Behandling forrigeBehandling = null;

        while (!ugyldigeReferanser.isEmpty()) {

            // Gjenopprett i kalkulus
            var kopierBeregningRequests = gjenopprettIKalkulus(saksnummer, fagsak, nesteBehandling, originalBehandling, ugyldigeReferanser);

            // Sett riktig initiell versjon i behandlingen som kommer etter i kronologisk rekkefølge
            // PS: Vi looper bakover i tid, så forrigeBehandling er opprettet etter nesteBehandling
            if (forrigeBehandling != null) {
                oppdaterInitiellVersjonForForrigeBehandling(forrigeBehandling, kopierBeregningRequests);
            }

            // Oppdaterer for neste iterasjon
            forrigeBehandling = nesteBehandling;
            nesteBehandling = originalBehandling;
            originalBehandling = behandlingRepository.hentBehandling(nesteBehandling.getOriginalBehandlingId().orElseThrow());
            ugyldigeReferanser = finnUgyldigeReferanserForBehandling(perioderTilVurderingTjeneste, nesteBehandling);
        }

    }

    private List<KopierBeregningRequest> gjenopprettIKalkulus(String saksnummer, Fagsak fagsak, Behandling nesteBehandling, Behandling originalBehandling, Map<LocalDate, UUID> ugyldigeReferanser) {
        var kopierBeregningRequests = ugyldigeReferanser.values().stream().map(r -> {
            var nyRef = new BgRef(null);
            return new KopierBeregningRequest(nyRef.getRef(), r);
        }).toList();
        var request = new KopierOgResettBeregningListeRequest(saksnummer,
            nesteBehandling.getUuid(),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(fagsak.getYtelseType()),
            StegType.FAST_BERGRUNN,
            kopierBeregningRequests,
            originalBehandling.getAvsluttetDato());
        kalkulusSystemRestKlient.kopierOgResettBeregning(request);
        return kopierBeregningRequests;
    }

    private Map<LocalDate, UUID> finnUgyldigeReferanserForBehandling(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, Behandling nesteBehandling) {
        var skjæringstidspunkterTilVurdering = finnSkjæringstidspunkterTilVurdering(nesteBehandling, perioderTilVurderingTjeneste);
        var vurderteEksternReferanserMap = finnVurderteReferanser(nesteBehandling.getId(), skjæringstidspunkterTilVurdering);
        var originaleVurderteReferanser = finnOriginaleVurderteReferanser(nesteBehandling, vurderteEksternReferanserMap);
        return finnLikeReferanser(vurderteEksternReferanserMap, originaleVurderteReferanser);
    }

    private void oppdaterInitiellVersjonForForrigeBehandling(Behandling forrigeBehandling, List<KopierBeregningRequest> kopierBeregningRequests) {
        var grunnlagInitiellVersjonOpt = beregningPerioderGrunnlagRepository.getInitiellVersjon(forrigeBehandling.getId());
        if (grunnlagInitiellVersjonOpt.isPresent()) {
            var initiellVersjon = grunnlagInitiellVersjonOpt.get();
            initiellVersjon.getGrunnlagPerioder()
                .forEach(p -> oppdaterInitiellVersjonForPeriodeHvisRelevant(kopierBeregningRequests, p));
        }
    }

    private void oppdaterInitiellVersjonForPeriodeHvisRelevant(List<KopierBeregningRequest> kopierBeregningRequests, BeregningsgrunnlagPeriode p) {
        var nyInitiellReferanse = finnNyInitiellReferanse(kopierBeregningRequests, p);
        if (nyInitiellReferanse.isPresent()) {
            p.setEksternReferanse(nyInitiellReferanse.get());
            entityManager.persist(p);
        }
    }

    private Optional<UUID> finnNyInitiellReferanse(List<KopierBeregningRequest> kopierBeregningRequests, BeregningsgrunnlagPeriode p) {
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
