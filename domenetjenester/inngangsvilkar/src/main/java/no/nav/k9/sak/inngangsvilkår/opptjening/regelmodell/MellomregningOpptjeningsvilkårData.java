package no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell;

import java.time.LocalDate;
import java.time.Period;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

/**
 * Data underlag inkludert mellomregning og mellomresultater brukt i vilkårsvurderingen.
 */
public class MellomregningOpptjeningsvilkårData {

    private final Map<Aktivitet, MellomregningAktivitetData> mellomregning = new HashMap<>();

    /** Beregnet total opptjening (inklusiv bekreftet og antatt) */
    private OpptjentTidslinje antattTotalOpptjening;

    /** Beregnet total opptjening (kun bekreftet). */
    private OpptjentTidslinje bekreftetTotalOpptjening;

    /** Beregnet total opptjening. */
    private OpptjentTidslinje totalOpptjening;

    /**
     * Opprinnelig grunnlag.
     */
    private Opptjeningsgrunnlag grunnlag;

    /** Frist for å motta opptjening opplysninger (henger sammen med Aksjonspunkt 7006 "Venter på Opptjeningsopplysninger"). */
    private LocalDate opptjeningOpplysningerFrist;

    private LocalDateTimeline<Boolean> perioderMedKunAAPogSN;

    public MellomregningOpptjeningsvilkårData(Opptjeningsgrunnlag grunnlag) {
        this.grunnlag = grunnlag;
        LocalDateInterval maxIntervall = grunnlag.getOpptjeningPeriode();

        mellomregning.put(new Aktivitet(Opptjeningsvilkår.MELLOM_ARBEID), new MellomregningAktivitetData(new Aktivitet(Opptjeningsvilkår.MELLOM_ARBEID)));

        // grupper aktivitet perioder etter aktivitet og avkort i forhold til angitt startDato/skjæringstidspunkt
        splitAktiviter(
            a -> a.getVurderingsStatus() == null)
                .forEach(e -> mellomregning.computeIfAbsent(e.getKey(),
                    a -> new MellomregningAktivitetData(a, e.getValue())));

        splitAktiviter(
            a -> Objects.equals(AktivitetPeriode.VurderingsStatus.TIL_VURDERING, a.getVurderingsStatus()))
                .forEach(e -> mellomregning.computeIfAbsent(e.getKey(),
                    a -> new MellomregningAktivitetData(a, e.getValue())));

        splitAktiviter(
            a -> Objects.equals(AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT, a.getVurderingsStatus()))
                .forEach(
                    e -> mellomregning.computeIfAbsent(e.getKey(),
                        MellomregningAktivitetData::new).setAktivitetUnderkjent(e.getValue()));

        // grupper inntektperioder etter aktivitet og avkort i forhold til angitt startDato/skjæringstidspunkt
        Map<Aktivitet, Set<LocalDateSegment<Long>>> grupperInntekterEtterAktiitet = grunnlag.getInntektPerioder().stream().collect(
            Collectors.groupingBy(InntektPeriode::getAktivitet,
                Collectors.mapping(a1 -> new LocalDateSegment<>(a1.getDatoInterval(), a1.getInntektBeløp()), Collectors.toSet())));

        LocalDateSegmentCombinator<Long, Long, Long> inntektOverlapDuplikatCombinator = StandardCombinators::sum;

        grupperInntekterEtterAktiitet
            .entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                new LocalDateTimeline<>(e.getValue(), inntektOverlapDuplikatCombinator).intersection(maxIntervall)))
            .filter(e -> !e.getValue().isEmpty())
            .forEach(
                e -> mellomregning.computeIfAbsent(e.getKey(),
                    MellomregningAktivitetData::new).setInntektTidslinjer(e.getValue()));

    }

    private Stream<Map.Entry<Aktivitet, LocalDateTimeline<Boolean>>> splitAktiviter(Predicate<AktivitetPeriode> filter) {
        Map<Aktivitet, Set<LocalDateSegment<Boolean>>> aktiviteter = grunnlag.getAktivitetPerioder().stream()
            .filter(filter)
            .collect(
                Collectors.groupingBy(AktivitetPeriode::getOpptjeningAktivitet,
                    Collectors.mapping(a -> new LocalDateSegment<>(a.getDatoInterval(), Boolean.TRUE), Collectors.toSet())));

        LocalDateSegmentCombinator<Boolean, Boolean, Boolean> aktivitetOverlappDuplikatCombinator = StandardCombinators::alwaysTrueForMatch;

        return aktiviteter
            .entrySet().stream()
            .map(e -> {
                return (Map.Entry<Aktivitet, LocalDateTimeline<Boolean>>) new AbstractMap.SimpleEntry<>(e.getKey(),
                    new LocalDateTimeline<>(e.getValue().stream().sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval)).collect(Collectors.toList()), aktivitetOverlappDuplikatCombinator));
            })
            .filter(e -> !e.getValue().isEmpty());
    }

    public Map<Aktivitet, LocalDateTimeline<Boolean>> getAkseptertMellomliggendePerioder() {
        return getMellomregningTidslinje(MellomregningAktivitetData::getAkseptertMellomliggendePerioder);
    }

    private <V> Map<Aktivitet, LocalDateTimeline<V>> getMellomregningTidslinje(Function<MellomregningAktivitetData, LocalDateTimeline<V>> fieldGetter) {
        return mellomregning.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), fieldGetter.apply(e.getValue())))
            .filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void setAkseptertMellomliggendePerioder(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder) {
        perioder.forEach((key, value) -> mellomregning.get(key).setAkseptertMellomliggendePerioder(value));
    }

    /**
     * Returnerer aktivitet tidslinjer, uten underkjente perioder (hvis satt), og med valgfritt med/uten antatt
     * godkjente perioder.
     */
    public Map<Aktivitet, LocalDateTimeline<Boolean>> getAktivitetTidslinjer(boolean medAntattGodkjentePerioder, boolean medIkkebekreftedeGodkjentePerioder) {

        Map<Aktivitet, LocalDateTimeline<Boolean>> resultat = mellomregning
            .entrySet().stream()
            .map(
                e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                    e.getValue().getAktivitetTidslinje(medAntattGodkjentePerioder, medIkkebekreftedeGodkjentePerioder)))
            .filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return resultat;

    }

    public boolean splitOgUnderkjennSegmenterEtterDatoForAktivitet(Aktivitet aktivitet, LocalDate splitDato) {

        if (splitDato.equals(grunnlag.getSisteDatoForOpptjening()) || mellomregning.get(aktivitet) == null) {
            return false;
        }
        LocalDateInterval underkjennIntervall = new LocalDateInterval(splitDato.plusDays(1), grunnlag.getSisteDatoForOpptjening());
        LocalDateTimeline<Boolean> underkjennTimeline = new LocalDateTimeline<>(underkjennIntervall, Boolean.TRUE);
        MellomregningAktivitetData aktivitetMellomregning = mellomregning.get(aktivitet);

        aktivitetMellomregning.setAktivitetUnderkjent(underkjennTimeline);
        return true;
    }

    private Map<Aktivitet, LocalDateTimeline<Boolean>> getAntattGodkjentPerioder() {
        return getMellomregningTidslinje(MellomregningAktivitetData::getAktivitetAntattGodkjent);
    }

    OpptjentTidslinje getAntattTotalOpptjening() {
        return antattTotalOpptjening;
    }

    OpptjentTidslinje getBekreftetOpptjening() {
        return bekreftetTotalOpptjening;
    }

    public Opptjeningsgrunnlag getGrunnlag() {
        return grunnlag;
    }

    Map<Aktivitet, LocalDateTimeline<Long>> getInntektTidslinjer() {
        return getMellomregningTidslinje(MellomregningAktivitetData::getInntektTidslinjer);
    }

    OpptjentTidslinje getTotalOpptjening() {
        return totalOpptjening;
    }

    public Map<Aktivitet, LocalDateTimeline<Boolean>> getUnderkjentePerioder() {
        return getMellomregningTidslinje(MellomregningAktivitetData::getAktivitetUnderkjent);
    }

    public void oppdaterOutputResultat(OpptjeningsvilkårResultat outputResultat) {
        /*
         * tar ikke med antatt godkjent, mellomliggende akseptert eller underkjent i aktivitet returnert her. De angis
         * separat under.
         */
        LocalDateInterval opptjeningPeriode = getGrunnlag().getOpptjeningPeriode();
        outputResultat.setBekreftetGodkjentAktivitet(trimTidslinje(this.getAktivitetTidslinjer(false, false), opptjeningPeriode));

        outputResultat.setUnderkjentePerioder(trimTidslinje(this.getUnderkjentePerioder(), opptjeningPeriode));
        outputResultat.setAntattGodkjentePerioder(trimTidslinje(this.getAntattGodkjentPerioder(), opptjeningPeriode));
        outputResultat.setAkseptertMellomliggendePerioder(trimTidslinje(this.getAkseptertMellomliggendePerioder(), opptjeningPeriode));

        /* hvis Oppfylt/Ikke Oppfylt (men ikke "Ikke Vurdert"), så angis total opptjening som er kalkulert. */
        outputResultat.setTotalOpptjening(this.getTotalOpptjening());
        outputResultat.setFrist(this.getOpptjeningOpplysningerFrist());
    }

    LocalDate getOpptjeningOpplysningerFrist() {
        return opptjeningOpplysningerFrist;
    }

    void setOpptjeningOpplysningerFrist(LocalDate opptjeningOpplysningerFrist) {
        this.opptjeningOpplysningerFrist = opptjeningOpplysningerFrist;
    }

    public void setAntattOpptjening(OpptjentTidslinje antattOpptjening) {
        this.antattTotalOpptjening = antattOpptjening;
    }

    public void setBekreftetTotalOpptjening(OpptjentTidslinje opptjening) {
        this.bekreftetTotalOpptjening = opptjening;
    }

    /**
     * Endelig valt opptjeningperiode.
     */
    void setTotalOpptjening(OpptjentTidslinje totalOpptjening) {
        this.totalOpptjening = totalOpptjening;
    }

    void setAntattGodkjentePerioder(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder) {
        perioder.forEach((key, value) -> mellomregning.get(key).setAktivitetAntattGodkjent(value));
    }

    void setUnderkjentePerioder(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder) {
        perioder.forEach((key, value) -> mellomregning.get(key).setAktivitetUnderkjent(value));
    }

    /**
     * Sjekker om opptjening er nok til å legge på vent ifht. konfigurert minste periode for vent.
     */
    boolean sjekkErInnenforMinsteGodkjentePeriodeForVent(Period opptjeningPeriode) {
        int minsteAntallMåneder = grunnlag.getMinsteAntallMånederForVent();
        int minsteAntallDager = grunnlag.getMinsteAntallDagerForVent();
        return sjekkErErOverAntallPåkrevd(opptjeningPeriode, minsteAntallMåneder, minsteAntallDager);
    }

    /**
     * Sjekker om opptjening er nok ifht. konfigurert minste periode.
     */
    boolean sjekkErInnenforMinstePeriodeGodkjent(Period opptjeningPeriode) {
        int minsteAntallMåneder = grunnlag.getMinsteAntallMånederGodkjent();
        int minsteAntallDager = grunnlag.getMinsteAntallDagerGodkjent();
        return sjekkErErOverAntallPåkrevd(opptjeningPeriode, minsteAntallMåneder, minsteAntallDager);
    }

    private static boolean sjekkErErOverAntallPåkrevd(Period opptjentPeriode, int minsteAntallMåneder,
                                                      int minsteAntallDager) {
        return opptjentPeriode.getMonths() > minsteAntallMåneder
            || (opptjentPeriode.getMonths() == minsteAntallMåneder && opptjentPeriode.getDays() >= minsteAntallDager);
    }

    private static Map<Aktivitet, LocalDateTimeline<Boolean>> trimTidslinje(Map<Aktivitet, LocalDateTimeline<Boolean>> tidslinjer,
                                                                            LocalDateInterval maxInterval) {
        return tidslinjer.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().intersection(maxInterval)))
            .filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public LocalDateTimeline<Boolean> getPerioderMedKunAAPogSN() {
        return perioderMedKunAAPogSN;
    }

    public void setPerioderMedKunAAPogSN(LocalDateTimeline<Boolean> perioderMedKunAAPogSN) {
        this.perioderMedKunAAPogSN = perioderMedKunAAPogSN;
    }
}
