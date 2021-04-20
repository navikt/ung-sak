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
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPOppgittOpptjeningFilter implements OppgittOpptjeningFilter {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private Boolean lansert;

    OMPOppgittOpptjeningFilter() {
        // For CDI
    }

    @Inject
    public OMPOppgittOpptjeningFilter(OmsorgspengerGrunnlagRepository grunnlagRepository,
                                      VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @KonfigVerdi(value = "MOTTAK_SOKNAD_UTBETALING_OMS", defaultVerdi = "true") Boolean lansert) {
        this.grunnlagRepository = grunnlagRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.lansert = lansert;
    }

    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        if (!lansert) {
            return iayGrunnlag.getOppgittOpptjening();
        }

        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));

        var fraværPerioderFraSøknad = grunnlagRepository.hentOppgittFraværFraSøknadHvisEksisterer(behandlingId).map(OppgittFravær::getPerioder).orElse(Set.of());
        var vilkårsperiode = finnVilkårsperiode(ref, stp);

        return finnOppgittOpptjening(iayGrunnlag, vilkårsperiode, fraværPerioderFraSøknad);
    }

    Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode, Set<OppgittFraværPeriode> fraværPerioderFraSøknad) {
        Map<JournalpostId, LocalDateTimeline<Void>> journalpostAktivTidslinje = beregnJournalpostAktivTidslinje(fraværPerioderFraSøknad);
        List<OppgittOpptjening> oppgittOpptjeninger = iayGrunnlag.getOppgittOpptjeningAggregat().map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger).orElse(List.of());

        return oppgittOpptjeninger.stream()
            .sorted(Comparator.comparing(OppgittOpptjening::getInnsendingstidspunkt)) // TODO: Avklare funksjonelt om dette er ønsket sortering
            .filter(oppgittOpptjening -> matcherVilkårsperiode(oppgittOpptjening, vilkårsperiode, journalpostAktivTidslinje))
            .findFirst();
    }

    private DatoIntervallEntitet finnVilkårsperiode(BehandlingReferanse ref, LocalDate stp) {
        var skalIgnorereAvslåttePerioder = true;
        return vilkårTjeneste.utledPerioderTilVurdering(ref, VilkårType.OPPTJENINGSVILKÅRET, skalIgnorereAvslåttePerioder)
            .stream()
            .filter(di -> di.getFomDato().equals(stp))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Utviklerfeil: Skjæringstidspunkt må ha matchende vilkårsperiode for opptjeningsvilkåret"));
    }

    private Map<JournalpostId, LocalDateTimeline<Void>> beregnJournalpostAktivTidslinje(Set<OppgittFraværPeriode> oppgittFraværPerioder) {
        var journalpostPerioder = oppgittFraværPerioder.stream()
            .collect(Collectors.groupingBy(OppgittFraværPeriode::getJournalpostId, Collectors.toSet()));

        var jornalpostTidslinjer = journalpostPerioder.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> slåSammenPerioder(e.getValue())));
        return jornalpostTidslinjer;
    }

    private boolean matcherVilkårsperiode(OppgittOpptjening opptjening, DatoIntervallEntitet vilkårsperiode, Map<JournalpostId, LocalDateTimeline<Void>> fraværTidslinjePerJp) {
        Objects.requireNonNull(opptjening.getJournalpostId());
        var fraværTidslinje = fraværTidslinjePerJp.getOrDefault(opptjening.getJournalpostId(), new LocalDateTimeline<>(List.of()));
        var overlappendeFraværsperioder = finnOverlappendePerioder(vilkårsperiode, fraværTidslinje);
        return !overlappendeFraværsperioder.isEmpty();
    }

    private NavigableSet<LocalDateInterval> finnOverlappendePerioder(DatoIntervallEntitet vilkårsperiode, LocalDateTimeline<Void> tidslinje) {
        return tidslinje.getLocalDateIntervals().stream()
            .map(di -> di.overlap(new LocalDateInterval(vilkårsperiode.getFomDato(), vilkårsperiode.getTomDato())))
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
