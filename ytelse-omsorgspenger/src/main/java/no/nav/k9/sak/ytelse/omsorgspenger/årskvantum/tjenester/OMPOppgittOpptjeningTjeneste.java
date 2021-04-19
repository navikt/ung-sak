package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPOppgittOpptjeningTjeneste implements OppgittOpptjeningTjeneste {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    private OMPOppgittOpptjeningTjeneste() {
        // For CDI
    }

    @Inject
    public OMPOppgittOpptjeningTjeneste(OmsorgspengerGrunnlagRepository grunnlagRepository,
                                        @FagsakYtelseTypeRef("OMP") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    // TODO Espen: Heller bruke stp enn opptjeningsperiode
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet opptjeningsperiode) {
        var stp = opptjeningsperiode.getTomDato().plusDays(1); // TODO: Litt dirty, iht. FastsettOpptjeningsperiode. Bør heller lagre stp eksplisitt på OpptjeningResultat#opptjeninger?
        return hentOppgittOpptjening(behandlingId, iayGrunnlag, stp);
    }

    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        Objects.requireNonNull(behandlingId);
        Map<JournalpostId, LocalDateTimeline<Void>> søktFraværTidslinjePerJp = beregnFraværTidslinje(behandlingId);
        var vilkårsperiode = finnVilkårsperiode(behandlingId, stp);

        // Søknadens journalpost refererer oppgitt opptjening OG søkte fraværsperioder
        // Første journalpost hvor fraværsperioder matcher vilkårsperiode blir returnert
        return iayGrunnlag.getOppgittOpptjeningAggregat()
            // TODO: Funksjonell avklaring. Fremste usikkerhet om dette blir riktig, er når bruker søker om samme periode én gang til ("korrigering")
            .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger).orElse(List.of())
            .stream()
            .sorted(Comparator.comparing(OppgittOpptjening::getInnsendingstidspunkt))
            .filter(oppgittOpptjening -> matcherVilkårsperiode(oppgittOpptjening, vilkårsperiode, søktFraværTidslinjePerJp))
            .findFirst();
    }

    private DatoIntervallEntitet finnVilkårsperiode(Long behandlingId, LocalDate stp) {
        return perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.OPPTJENINGSVILKÅRET).stream()
            .filter(it -> it.getFomDato().equals(stp))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Utviklerfeil: Skjæringstidspunkt må ha matchende vilkårsperiode for opptjeningsvilkåret"));
    }

    private Map<JournalpostId, LocalDateTimeline<Void>> beregnFraværTidslinje(Long behandlingId) {
        var oppgittFraværPerioder = grunnlagRepository.hentOppgittFraværFraSøknadHvisEksisterer(behandlingId)
            .map(OppgittFravær::getPerioder)
            .orElse(Set.of());
        var fraværPerioderPerJp = oppgittFraværPerioder.stream()
            .filter(fp -> Objects.nonNull(fp.getJournalpostId()))
            .collect(Collectors.groupingBy(OppgittFraværPeriode::getJournalpostId, Collectors.toSet()));
        var fraværTidslinjePerJp = fraværPerioderPerJp.entrySet().stream()
            .map(e -> {
                Set<OppgittFraværPeriode> fraværPerioderForJp = e.getValue();
                LocalDateTimeline<Void> tidslinjeForJp = slåSammenPerioder(fraværPerioderForJp);
                return Map.entry(e.getKey(), tidslinjeForJp);
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fraværTidslinjePerJp;
    }

    private boolean matcherVilkårsperiode(OppgittOpptjening opptjening, DatoIntervallEntitet vilkårsperiode, Map<JournalpostId, LocalDateTimeline<Void>> fraværTidslinjePerJp) {
        Objects.requireNonNull(opptjening.getJournalpostId());
        var fraværTidslinje = fraværTidslinjePerJp.getOrDefault(opptjening.getJournalpostId(), new LocalDateTimeline<>(List.of()));
        var overlappendeFraværsperioder = finnOverlappendePerioder(vilkårsperiode, fraværTidslinje);
        return !overlappendeFraværsperioder.isEmpty();
    }

    private NavigableSet<LocalDateInterval> finnOverlappendePerioder(DatoIntervallEntitet datoIntervall, LocalDateTimeline<Void> tidslinje) {
        return tidslinje.getLocalDateIntervals().stream()
            .map(it -> it.overlap(new LocalDateInterval(datoIntervall.getFomDato(), datoIntervall.getTomDato())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Void> slåSammenPerioder(Set<OppgittFraværPeriode> oppgittFraværPerioder) {
        var fraværPerioder = oppgittFraværPerioder.stream()
            .map(it -> new LocalDateSegment<Void>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), null))
            .collect(Collectors.toSet());
        var tilkjentYtelseTimeline = new LocalDateTimeline<Void>(List.of());
        for (LocalDateSegment<Void> periode : fraværPerioder) {
            tilkjentYtelseTimeline = tilkjentYtelseTimeline.combine(new LocalDateTimeline<>(List.of(periode)), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return tilkjentYtelseTimeline.compress();
    }

}
