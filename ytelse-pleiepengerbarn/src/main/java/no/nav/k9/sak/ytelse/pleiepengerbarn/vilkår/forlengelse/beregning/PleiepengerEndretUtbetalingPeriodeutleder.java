package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static java.lang.Boolean.TRUE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndretUtbetalingPeriodeutleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@ApplicationScoped
public class PleiepengerEndretUtbetalingPeriodeutleder implements EndretUtbetalingPeriodeutleder {

    private UttakTjeneste uttakRestKlient;
    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    private SøknadsperiodeRepository søknadsperiodeRepository;
    private boolean enabled;


    public PleiepengerEndretUtbetalingPeriodeutleder() {
    }

    @Inject
    public PleiepengerEndretUtbetalingPeriodeutleder(UttakTjeneste uttakRestKlient,
                                                     BehandlingRepository behandlingRepository,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                                     SøknadsperiodeRepository søknadsperiodeRepository,
                                                     @KonfigVerdi(value = "BG_FORLENGELSE_BASERT_PAA_UTTAK", defaultVerdi = "false") boolean enabled) {
        this.uttakRestKlient = uttakRestKlient;
        this.behandlingRepository = behandlingRepository;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.enabled = enabled;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse) {
        var periodeTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        return periodeTjeneste.utled(behandlingReferanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR).stream()
            .flatMap(p -> utledPerioder(behandlingReferanse, p).stream())
            .collect(Collectors.toCollection(TreeSet::new));

    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {

        if (!enabled) {
            return new TreeSet<>(Set.of(periode));
        }

        var søknadperioderForBehandlingTidslinje = finnTidslinjeForRelevanteSøknadsperioder(behandlingReferanse);
        var relevantUttaksTidslinje = finnTidslinjeForEndredeUttaksperioder(behandlingReferanse, periode);
        return relevantUttaksTidslinje.crossJoin(søknadperioderForBehandlingTidslinje, StandardCombinators::coalesceLeftHandSide)
            .compress()
            .toSegments()
            .stream()
            .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForRelevanteSøknadsperioder(BehandlingReferanse behandlingReferanse) {
        var søknadsperiodeGrunnlag = søknadsperiodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());

        var søknadsperioderForBehandling = søknadsperiodeGrunnlag.map(SøknadsperiodeGrunnlag::getRelevantSøknadsperioder)
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .flatMap(p -> p.getPerioder().stream())
            .map(Søknadsperiode::getPeriode)
            .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), TRUE)).toList();

        return new LocalDateTimeline<>(søknadsperioderForBehandling, StandardCombinators::coalesceLeftHandSide);
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForEndredeUttaksperioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        var relevanteUttaksperioder = finnRelevanteUttaksperioder(behandlingReferanse, periode);

        return new LocalDateTimeline<>(relevanteUttaksperioder.stream().map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), TRUE)).toList());
    }

    private NavigableSet<DatoIntervallEntitet> finnRelevanteUttaksperioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        var originalBehandlingId = behandlingReferanse.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Forventer å finne original behandling"));
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);

        var uttaksplan = uttakRestKlient.hentUttaksplan(behandlingReferanse.getBehandlingUuid(), true);
        var originalUttakslpan = uttakRestKlient.hentUttaksplan(originalBehandling.getUuid(), true);

        var uttakTidslinje = lagTidslinje(uttaksplan);
        var originalUttakTidslinje = lagTidslinje(originalUttakslpan);

        // Må bruke difference begge veier for å finne både nye arbeidsforhold og eventuelt fjernede arbeidsforhold
        var differanse1 = uttakTidslinje.combine(originalUttakTidslinje, StandardCombinators::difference, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        var differanse2 = originalUttakTidslinje.combine(uttakTidslinje, StandardCombinators::difference, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        var relevanteUttaksperioder = new TreeSet<DatoIntervallEntitet>();
        relevanteUttaksperioder.addAll(finnRelevanteIntervaller(periode, differanse1));
        relevanteUttaksperioder.addAll(finnRelevanteIntervaller(periode, differanse2));
        return relevanteUttaksperioder;
    }

    private NavigableSet<DatoIntervallEntitet> finnRelevanteIntervaller(DatoIntervallEntitet periode, LocalDateTimeline<Set<Utbetalingsgrader>> differanse1) {
        var kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

        var intervaller1 = differanse1.toSegments().stream()
            .filter(s -> !s.getValue().isEmpty())
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()))
            .sorted(Comparator.naturalOrder())
            .toList();

        var resultat1 = new TreeSet<DatoIntervallEntitet>();

        for (var intervall : intervaller1) {
            if (intervall.overlapper(periode)) {
                resultat1.add(intervall);
            } else if (resultat1.stream().anyMatch(r -> kantIKantVurderer.erKantIKant(intervall, r)) || kantIKantVurderer.erKantIKant(intervall, periode)) {
                resultat1.add(intervall);
            }
        }
        return resultat1;
    }

    private LocalDateTimeline<Set<Utbetalingsgrader>> lagTidslinje(Uttaksplan uttaksplan) {
        Set<LocalDateSegment<Set<Utbetalingsgrader>>> segmenter = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .map(e -> new LocalDateSegment<>(e.getKey().getFom(), e.getKey().getTom(),
                e.getValue().getUtbetalingsgrader().stream().collect(Collectors.toSet())))
            .collect(Collectors.toSet());

        return new LocalDateTimeline<>(segmenter);
    }


}
