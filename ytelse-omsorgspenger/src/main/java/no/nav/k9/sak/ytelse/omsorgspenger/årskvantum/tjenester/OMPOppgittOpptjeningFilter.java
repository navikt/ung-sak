package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.time.LocalDate;
import java.util.Collection;
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
import no.nav.k9.kodeverk.dokument.DokumentStatus;
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
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.SøknadPerioderTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPOppgittOpptjeningFilter implements OppgittOpptjeningFilter {

    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadPerioderTjeneste søknadPerioderTjeneste;
    private Boolean lansert;

    OMPOppgittOpptjeningFilter() {
        // For CDI
    }

    @Inject
    public OMPOppgittOpptjeningFilter(VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      MottatteDokumentRepository mottatteDokumentRepository,
                                      SøknadPerioderTjeneste søknadPerioderTjeneste,
                                      OppgittOpptjeningMapper oppgittOpptjeningMapper,
                                      @KonfigVerdi(value = "MOTTAK_SOKNAD_UTBETALING_OMS", defaultVerdi = "true") Boolean lansert) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.lansert = lansert;
    }

    /**
     * Henter sist mottatte oppgitte opptjening per aktivitetskategori (SN, FL, AT) hvis respektive journalpost matcher
     * fraværsperiode som overlapper opptjeningvilkårets vilkårsperiode
     * Stp brukes for å finne vilkårsperidoe
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        if (!lansert) {
            return iayGrunnlag.getOppgittOpptjening();
        }

        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));

        var gyldigeJournalposter = hentGyldigeDokumenter(ref);
        var fraværPerioderFraSøknad = hentSøknadsperioderPåFagsak(ref);
        var vilkårsperiode = finnVilkårsperiodeForOpptjening(ref, stp);

        return finnOppgittOpptjening(iayGrunnlag, vilkårsperiode, fraværPerioderFraSøknad, gyldigeJournalposter);
    }

    /**
     * Henter sist mottatte oppgitte opptjening per aktivitetskategori (SN, FL, AT) hvis respektive journalpost matcher
     * fraværsperiode som overlapper opptjeningvilkårets vilkårsperiode
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        if (!lansert) {
            return iayGrunnlag.getOppgittOpptjening();
        }

        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));
        var gyldigeJournalposter = hentGyldigeDokumenter(ref);
        var fraværPerioderFraSøknad = hentSøknadsperioderPåFagsak(ref);

        return finnOppgittOpptjening(iayGrunnlag, vilkårsperiode, fraværPerioderFraSøknad, gyldigeJournalposter);
    }

    Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode, Set<OppgittFraværPeriode> fraværPerioderFraSøknad, Map<JournalpostId, MottattDokument> gyldigeJournalposter) {
        Map<JournalpostId, LocalDateTimeline<Void>> journalpostAktivTidslinje = beregnJournalpostAktivTidslinje(fraværPerioderFraSøknad);
        List<WrappedOppgittOpptjening> oppgitteOpptjeninger = sorterOpptjeningerMotJournalpost(iayGrunnlag, gyldigeJournalposter);

        List<OppgittOpptjening> overlappendeOpptjeninger = hentOverlappendeOpptjeninger(vilkårsperiode, journalpostAktivTidslinje, oppgitteOpptjeninger);
        if (overlappendeOpptjeninger.isEmpty()) {
            return Optional.empty();
        }

        var oppgittOpptjening = OppgittOpptjeningMapper.sammenstillOppgittOpptjening(overlappendeOpptjeninger);
        return Optional.of(oppgittOpptjening);
    }

    private Map<JournalpostId, MottattDokument> hentGyldigeDokumenter(BehandlingReferanse ref) {
        var gyldigeJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(ref.getFagsakId())
            .stream()
            .filter(it -> DokumentStatus.GYLDIG.equals(it.getStatus()))
            .filter(it -> it.getBehandlingId() != null)
            .collect(Collectors.toMap(e -> e.getJournalpostId(), e -> e));
        return gyldigeJournalposter;
    }

    private Set<OppgittFraværPeriode> hentSøknadsperioderPåFagsak(BehandlingReferanse ref) {
        return søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåFagsak(ref).values()
            .stream()
            .flatMap(Collection::stream)
            .map(it -> it.getRaw())
            .collect(Collectors.toSet());
    }

    private List<OppgittOpptjening> hentOverlappendeOpptjeninger(DatoIntervallEntitet vilkårsperiode, Map<JournalpostId, LocalDateTimeline<Void>> journalpostAktivTidslinje, List<WrappedOppgittOpptjening> oppgittOpptjeninger) {
        var overlappendeOpptjeninger = oppgittOpptjeninger.stream()
            .filter(opptj -> overlapperVilkårsperiode(opptj, vilkårsperiode, journalpostAktivTidslinje))
            .map(WrappedOppgittOpptjening::getRaw)
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

    private Map<JournalpostId, LocalDateTimeline<Void>> beregnJournalpostAktivTidslinje(Set<OppgittFraværPeriode> oppgittFraværPerioder) {
        var journalpostPerioder = oppgittFraværPerioder.stream()
            .collect(Collectors.groupingBy(OppgittFraværPeriode::getJournalpostId, Collectors.toSet()));

        var jornalpostTidslinjer = journalpostPerioder.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> slåSammenPerioder(e.getValue())));
        return jornalpostTidslinjer;
    }

    private List<WrappedOppgittOpptjening> sorterOpptjeningerMotJournalpost(InntektArbeidYtelseGrunnlag iay, Map<JournalpostId, MottattDokument> gyldigeJournalposter) {
        var oppgitteOpptjeninger = iay.getOppgittOpptjeningAggregat()
            .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger)
            .orElse(List.of());

        var oppgitteOpptjeningerSortert = oppgitteOpptjeninger
            .stream()
            .filter(opptjening -> gyldigeJournalposter.containsKey(opptjening.getJournalpostId()))
            .map(opptjening -> {
                var mottattDokument = gyldigeJournalposter.get(opptjening.getJournalpostId());
                return new WrappedOppgittOpptjening(mottattDokument.getJournalpostId(), mottattDokument.getInnsendingstidspunkt(), opptjening);
            })
            .sorted(Comparator.comparing(WrappedOppgittOpptjening::getInnsendingstidspunkt, Comparator.reverseOrder()))
            .collect(Collectors.toList());
        return oppgitteOpptjeningerSortert;
    }

    private boolean overlapperVilkårsperiode(WrappedOppgittOpptjening opptjening, DatoIntervallEntitet vilkårsperiode, Map<JournalpostId, LocalDateTimeline<Void>> fraværTidslinjePerJp) {
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
