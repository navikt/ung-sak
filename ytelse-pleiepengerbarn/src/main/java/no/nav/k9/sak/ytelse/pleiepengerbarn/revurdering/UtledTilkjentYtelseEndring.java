package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class UtledTilkjentYtelseEndring {


    private BeregningsresultatRepository beregningsresultatRepository;

    public UtledTilkjentYtelseEndring() {
    }

    @Inject
    public UtledTilkjentYtelseEndring(BeregningsresultatRepository beregningsresultatRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    public List<EndringerForMottaker> utledEndringer(BehandlingReferanse behandlingReferanse) {


        var beregningsresultatEntitet = beregningsresultatRepository.hentEndeligBeregningsresultat(behandlingReferanse.getBehandlingId());
        if (beregningsresultatEntitet.isEmpty()) {
            throw new IllegalStateException("Kan ikke utlede endring i tilkjent ytelse uten å ha lagret tilkjent ytelse");
        }

        var originalBeregningsresultatEntitet = beregningsresultatRepository.hentEndeligBeregningsresultat(behandlingReferanse.getOriginalBehandlingId().orElseThrow(() -> new IllegalArgumentException("Kan ikke utlede endringer dersom behandling ikke er revurdering")));
        var utbetalingTidslinje = lagResultatTidslinje(beregningsresultatEntitet);
        var originalUtbetalingTidslinje = lagResultatTidslinje(originalBeregningsresultatEntitet);
        var endringstidslinje = utbetalingTidslinje.combine(originalUtbetalingTidslinje, UtledTilkjentYtelseEndring::finnEndring, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        var nøkler = finnAlleUnikeNøklerMedEndring(endringstidslinje);
        return nøkler.stream().map(n -> new EndringerForMottaker(n, finnEndringstidslinjeForNøkkel(n, endringstidslinje))).toList();
    }

    private static Set<MottakerNøkkel> finnAlleUnikeNøklerMedEndring(LocalDateTimeline<List<TilkjentYtelseEndretAndel>> endringstidslinje) {
        return endringstidslinje.stream()
            .flatMap(s -> s.getValue().stream().map(TilkjentYtelseEndretAndel::nøkkel))
            .collect(Collectors.toSet());
    }

    private static LocalDateTimeline<Endringer> finnEndringstidslinjeForNøkkel(MottakerNøkkel nøkkel, LocalDateTimeline<List<TilkjentYtelseEndretAndel>> endringstidslinje) {
        return endringstidslinje
            .mapValue(value -> finnEndringForNøkkel(nøkkel, value))
            .filterValue(Optional::isPresent)
            .mapValue(Optional::get);
    }

    private static Optional<Endringer> finnEndringForNøkkel(MottakerNøkkel nøkkel, List<TilkjentYtelseEndretAndel> value) {
        return value.stream().filter(endring -> endring.nøkkel().equals(nøkkel)).findFirst().map(TilkjentYtelseEndretAndel::endringer);
    }

    private static LocalDateSegment<List<TilkjentYtelseEndretAndel>> finnEndring(LocalDateInterval di,
                                                                                 LocalDateSegment<TilkjentYtelsePeriodeInnhold> gjeldende,
                                                                                 LocalDateSegment<TilkjentYtelsePeriodeInnhold> forrige) {
        if (gjeldende != null && forrige != null) {
            var gjeldendeVerdi = gjeldende.getValue();
            var forrigeVerdi = forrige.getValue();
            return new LocalDateSegment<>(di, finnEndringForPeriode(gjeldendeVerdi, forrigeVerdi));
        }

        if (gjeldende != null) {
            return new LocalDateSegment<>(di, finnEndringForNyPeriode(gjeldende.getValue()));
        }

        // Er det mulig å ende opp her? dvs at vi har fjernet en periode i tilkjent ytelse
        return null;

    }

    private static List<TilkjentYtelseEndretAndel> finnEndringForPeriode(TilkjentYtelsePeriodeInnhold gjeldendeVerdi, TilkjentYtelsePeriodeInnhold forrigeVerdi) {
        return finnAndelsendringer(gjeldendeVerdi, Optional.of(forrigeVerdi));
    }

    private static List<TilkjentYtelseEndretAndel> finnEndringForNyPeriode(TilkjentYtelsePeriodeInnhold gjeldendeVerdi) {
        return finnAndelsendringer(gjeldendeVerdi, Optional.empty());
    }

    private static List<TilkjentYtelseEndretAndel> finnAndelsendringer(TilkjentYtelsePeriodeInnhold gjeldendeVerdi, Optional<TilkjentYtelsePeriodeInnhold> forrigeVerdi) {
        var andelsEndringer = finnAndelsendringerFraGjeldende(gjeldendeVerdi, forrigeVerdi);
        var fjernetAndeler = forrigeVerdi.map(v -> finnFjernedeAndeler(v, gjeldendeVerdi)).orElse(Collections.emptyList());
        andelsEndringer.addAll(fjernetAndeler);
        return andelsEndringer;
    }

    private static List<TilkjentYtelseEndretAndel> finnFjernedeAndeler(TilkjentYtelsePeriodeInnhold forrigeVerdi, TilkjentYtelsePeriodeInnhold gjeldendeVerdi) {
        return forrigeVerdi.beregningsresultatAndelList().stream().filter(a ->
                gjeldendeVerdi.beregningsresultatAndelList().stream().noneMatch(a2 -> a2.getAktivitetsnøkkel().equals(a.getAktivitetsnøkkel())))
            .map(UtledTilkjentYtelseEndring::fjernetAndel)
            .toList();
    }

    private static TilkjentYtelseEndretAndel fjernetAndel(BeregningsresultatAndel a) {
        return new TilkjentYtelseEndretAndel(
            lagMottakerNøkkel(a),
            new Endringer(negate(a.getDagsats()), a.getUtbetalingsgrad().negate(), a.getFeriepengerÅrsbeløp().getVerdi().negate()));
    }

    @NotNull
    private static MottakerNøkkel lagMottakerNøkkel(BeregningsresultatAndel a) {
        return new MottakerNøkkel(a.erBrukerMottaker(), new Aktivitetsnøkkel(a.getArbeidsgiver().orElse(null), a.getArbeidsforholdRef(), a.getAktivitetStatus(), a.getInntektskategori()));
    }

    private static int negate(int dagsats) {
        return dagsats * -1;
    }

    private static ArrayList<TilkjentYtelseEndretAndel> finnAndelsendringerFraGjeldende(TilkjentYtelsePeriodeInnhold gjeldendeVerdi, Optional<TilkjentYtelsePeriodeInnhold> forrigeVerdi) {
        var andelsEndringer = new ArrayList<TilkjentYtelseEndretAndel>();

        gjeldendeVerdi.beregningsresultatAndelList().forEach(a -> {
            var aktivitetsnøkkel = a.getAktivitetsnøkkel();

            var matchFraForrige = forrigeVerdi.stream().flatMap(v -> v.beregningsresultatAndelList().stream().filter(a2 ->
                    a2.getAktivitetsnøkkel().equals(aktivitetsnøkkel)
                        && a2.erBrukerMottaker() == a.erBrukerMottaker()))
                .findFirst();

            matchFraForrige.ifPresentOrElse(a2 -> {
                    var endring = finnEndring(a, a2);
                    endring.ifPresent(andelsEndringer::add);
                },
                () -> andelsEndringer.add(nyAndel(a))
            );

        });
        return andelsEndringer;
    }

    private static TilkjentYtelseEndretAndel nyAndel(BeregningsresultatAndel a) {
        return new TilkjentYtelseEndretAndel(lagMottakerNøkkel(a), new Endringer(a.getDagsats(), a.getUtbetalingsgrad(), a.getFeriepengerÅrsbeløp().getVerdi()));
    }

    private static Optional<TilkjentYtelseEndretAndel> finnEndring(BeregningsresultatAndel a, BeregningsresultatAndel a2) {
        var endringDagsats = a.getDagsats() - a2.getDagsats();
        var endringUtbetalingsgrad = a.getUtbetalingsgrad().subtract(a2.getUtbetalingsgrad());
        var endringFeriepenger = a.getFeriepengerÅrsbeløp().getVerdi().subtract(a2.getFeriepengerÅrsbeløp().getVerdi());
        if (endringDagsats != 0 || endringUtbetalingsgrad.compareTo(BigDecimal.ZERO) != 0 || endringFeriepenger.compareTo(BigDecimal.ZERO) != 0) {
            return Optional.of(new TilkjentYtelseEndretAndel(lagMottakerNøkkel(a), new Endringer(endringDagsats, endringUtbetalingsgrad, endringFeriepenger)));
        }

        return Optional.empty();
    }

    private static LocalDateTimeline<TilkjentYtelsePeriodeInnhold> lagResultatTidslinje(Optional<BeregningsresultatEntitet> beregningsresultatEntitet) {
        var segmenter = beregningsresultatEntitet.stream()
            .flatMap(br -> br.getBeregningsresultatPerioder().stream())
            .map(p -> new LocalDateSegment<>(p.getPeriode().toLocalDateInterval(), new TilkjentYtelsePeriodeInnhold(
                p.getBeregningsresultatAndelList()
            ))).toList();

        return new LocalDateTimeline<>(segmenter);
    }


    public record TilkjentYtelsePeriodeInnhold(
        List<BeregningsresultatAndel> beregningsresultatAndelList
    ) {
    }

    public record TilkjentYtelseEndretAndel(
        MottakerNøkkel nøkkel,
        Endringer endringer
    ) {
    }

    public record MottakerNøkkel(Boolean brukerErMottaker,
                                 Aktivitetsnøkkel aktivitetsnøkkel) {
    }

    public record Endringer(
        int dagsatsEndring,
        BigDecimal utbetalingsgradEndring,
        BigDecimal feriepengerBeløpEndring
    ) {

    }

    public record EndringerForMottaker(
        MottakerNøkkel nøkkel,
        LocalDateTimeline<Endringer> endringer
    ) {
    }

    public record Aktivitetsnøkkel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef,
                                   AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori) {

        @Override
        public InternArbeidsforholdRef arbeidsforholdRef() {
            return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
        }
    }


}
