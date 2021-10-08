package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBOppgittOpptjeningFilter implements OppgittOpptjeningFilter {

    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private boolean lansertNyPrioritering;

    PSBOppgittOpptjeningFilter() {
        // For CDI
    }

    @Inject
    public PSBOppgittOpptjeningFilter(VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                      @KonfigVerdi(value = "PSB_OPPG_OPPTJENING_PRIORITERING", defaultVerdi = "true") boolean lansertNyPrioritering) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.lansertNyPrioritering = lansertNyPrioritering;
    }

    /**
     * Henter sist mottatte oppgitte opptjening som kan knyttes til vilkårsperiode (vilkårsperiode slås opp gjennom stp)
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));
        var vilkårsperiode = finnVilkårsperiodeForOpptjening(ref, stp);
        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokMedFravær = søknadsfristTjeneste.hentPerioderTilVurdering(ref);
        if (lansertNyPrioritering) {
            finnOppgittOpptjeningLansert(iayGrunnlag, vilkårsperiode, kravdokMedFravær);
        }
        return finnOppgittOpptjening(iayGrunnlag, vilkårsperiode, kravdokMedFravær);
    }

    /**
     * Henter sist mottatte oppgitte opptjening som kan knyttes til vilkårsperiode
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));
        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokMedFravær = søknadsfristTjeneste.hentPerioderTilVurdering(ref);
        if (lansertNyPrioritering) {
            finnOppgittOpptjeningLansert(iayGrunnlag, vilkårsperiode, kravdokMedFravær);
        }
        return finnOppgittOpptjening(iayGrunnlag, vilkårsperiode, kravdokMedFravær);
    }

    // Kode for lansering av ny prioriering - START
    Optional<OppgittOpptjening> finnOppgittOpptjeningLansert(InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode, Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær) {
        var stp = vilkårsperiode.getFomDato();
        var sistMottatteSøknadNærmestStp = kravDokumenterMedFravær.entrySet()
            .stream()
            .filter(it -> !it.getValue().isEmpty())
            .min((e1, e2) -> {
                var kravdok1 = e1.getKey();
                var kravdok2 = e2.getKey();
                var søktePerioder1 = e1.getValue();
                var søktePerioder2 = e2.getValue();

                var compareStpDistanse = Long.compare(distanseTilStp(søktePerioder1, stp), distanseTilStp(søktePerioder2, stp));
                if (compareStpDistanse != 0) {
                    return compareStpDistanse;
                }
                return kravdok2.getInnsendingsTidspunkt().compareTo(kravdok1.getInnsendingsTidspunkt());
            })
            .orElseThrow();
        var journalpostId = sistMottatteSøknadNærmestStp.getKey().getJournalpostId();

        var oppgitteOpptjeninger = iayGrunnlag.getOppgittOpptjeningAggregat()
            .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger)
            .orElse(List.of());
        return oppgitteOpptjeninger.stream()
            .filter(jp -> jp.getJournalpostId().equals(journalpostId))
            .findFirst();
    }

    private long distanseTilStp(List<SøktPeriode<Søknadsperiode>> søktePerioder, LocalDate skjæringstidspunkt) {
        var næresteDatoFraEksisterende = finnDatoNærmestSkjæringstidspunktet(søktePerioder, skjæringstidspunkt);
        long dist = Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, næresteDatoFraEksisterende));
        return dist;
    }

    private LocalDate finnDatoNærmestSkjæringstidspunktet(List<SøktPeriode<Søknadsperiode>> søktePerioder, LocalDate stp) {
        var inkludert = søktePerioder.stream()
            .filter(p -> p.getPeriode().inkluderer(stp))
            .findFirst();
        if (inkludert.isPresent()) {
            return stp;
        }
        return søktePerioder.stream()
            .map(it -> it.getPeriode().getFomDato())
            .min(Comparator.comparingLong(x -> Math.abs(ChronoUnit.DAYS.between(stp, x))))
            .orElseThrow();
    }
    // Kode for lansering av prioriering - END

    // Resten av kode er duplisert fra OMP, slik at oppførsel er uendret dersom ulansert
    Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode, Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær) {
        var journalpostAktivTidslinje = utledJournalpostAktivTidslinje(kravDokumenterMedFravær);
        List<OppgittOpptjening> oppgitteOpptjeninger = sorterOpptjeningerMotInnsendingstidspunkt(iayGrunnlag, kravDokumenterMedFravær);

        List<OppgittOpptjening> overlappendeOpptjeninger = hentOverlappendeOpptjeninger(vilkårsperiode, journalpostAktivTidslinje, oppgitteOpptjeninger);
        if (overlappendeOpptjeninger.isEmpty()) {
            return Optional.empty();
        }

        var oppgittOpptjening = OppgittOpptjeningMapper.sammenstillOppgittOpptjening(overlappendeOpptjeninger);
        return Optional.of(oppgittOpptjening);
    }

    private List<OppgittOpptjening> hentOverlappendeOpptjeninger(DatoIntervallEntitet vilkårsperiode, Map<JournalpostId, LocalDateTimeline<Void>> journalpostAktivTidslinje, List<OppgittOpptjening> oppgittOpptjeninger) {
        var overlappendeOpptjeninger = oppgittOpptjeninger.stream()
            .filter(opptj -> overlapperVilkårsperiode(opptj, vilkårsperiode, journalpostAktivTidslinje))
            .collect(Collectors.toList());
        return overlappendeOpptjeninger;
    }

    private DatoIntervallEntitet finnVilkårsperiodeForOpptjening(BehandlingReferanse ref, LocalDate stp) {
        var skalIgnorereAvslåttePerioder = false;
        var periodeTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, VilkårType.OPPTJENINGSVILKÅRET, skalIgnorereAvslåttePerioder)
            .stream()
            .filter(di -> di.getFomDato().equals(stp))
            .findFirst();
        if (periodeTilVurdering.isPresent()) {
            return periodeTilVurdering.get();
        }

        // Ingen match for stp mot perioder under vurdering -> må da forvente at den matcher vilkårsperiode som er ferdigvurdert
        var vilkår = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow();
        var periodeFerdigvurdert = vilkår.finnPeriodeForSkjæringstidspunkt(stp);
        if (periodeFerdigvurdert.getGjeldendeUtfall().equals(Utfall.IKKE_VURDERT)) {
            throw new IllegalStateException("Forventer at vilkårsperiode som matchet opptjening var ferdigvurdert");
        }
        return periodeFerdigvurdert.getPeriode();
    }

    private Map<JournalpostId, LocalDateTimeline<Void>> utledJournalpostAktivTidslinje(Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterPåFagsak) {
        return kravDokumenterPåFagsak.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().getJournalpostId(), e -> slåSammenPerioder(e.getValue())));
    }

    private List<OppgittOpptjening> sorterOpptjeningerMotInnsendingstidspunkt(InntektArbeidYtelseGrunnlag iay, Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokMedFravær) {
        var journalpostIdTilKravdok = kravdokMedFravær.keySet().stream()
            .collect(Collectors.toMap(e -> e.getJournalpostId(), e -> e));

        var oppgitteOpptjeninger = iay.getOppgittOpptjeningAggregat()
            .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger)
            .orElse(List.of());
        var oppgitteOpptjeningerSortert = oppgitteOpptjeninger
            .stream()
            .filter(opptjening -> journalpostIdTilKravdok.containsKey(opptjening.getJournalpostId()))
            .map(opptjening -> {
                var dok = journalpostIdTilKravdok.get(opptjening.getJournalpostId());
                return new WrappedOppgittOpptjening(dok.getJournalpostId(), dok.getInnsendingsTidspunkt(), opptjening);
            })
            // Sist mottatte sorteres først
            .sorted(Comparator.comparing(WrappedOppgittOpptjening::getInnsendingstidspunkt, Comparator.reverseOrder()))
            .map(WrappedOppgittOpptjening::getRaw)
            .collect(Collectors.toList());
        return oppgitteOpptjeningerSortert;
    }

    private boolean overlapperVilkårsperiode(OppgittOpptjening opptjening, DatoIntervallEntitet vilkårsperiode, Map<JournalpostId, LocalDateTimeline<Void>> fraværTidslinjePerJp) {
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

    private LocalDateTimeline<Void> slåSammenPerioder(List<SøktPeriode<Søknadsperiode>> søktePerioder) {
        var søktPeriode = søktePerioder.stream()
            .map(it -> new LocalDateSegment<Void>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), null))
            .collect(Collectors.toSet());
        var mergetSøktePerioder = new LocalDateTimeline<Void>(List.of());
        for (LocalDateSegment<Void> periode : søktPeriode) {
            mergetSøktePerioder = mergetSøktePerioder.combine(new LocalDateTimeline<>(List.of(periode)), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return mergetSøktePerioder.compress();
    }


}
