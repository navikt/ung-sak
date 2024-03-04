package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        var beregningsresultatEntitet = beregningsresultatRepository.hentEndeligBeregningsresultat(behandlingReferanse.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Kan ikke utlede endring i tilkjent ytelse uten å ha lagret tilkjent ytelse"));
        var originalBeregningsresultatEntitet = beregningsresultatRepository.hentEndeligBeregningsresultat(behandlingReferanse.getOriginalBehandlingId().orElseThrow(() -> new IllegalArgumentException("Kan ikke utlede tidslinjeMedEndringIYtelse dersom behandling ikke er revurdering")))
            .orElseThrow(() -> new IllegalArgumentException("Original behandling har ikke tilkjent ytelse"));
        return utledEndringer(beregningsresultatEntitet, originalBeregningsresultatEntitet);
    }

    static List<EndringerForMottaker> utledEndringer(BeregningsresultatEntitet beregningsresultatEntitet, BeregningsresultatEntitet originalBeregningsresultatEntitet) {
        var utbetalingTidslinje = lagResultatTidslinje(beregningsresultatEntitet);
        var originalUtbetalingTidslinje = lagResultatTidslinje(originalBeregningsresultatEntitet);
        var endringstidslinje = utbetalingTidslinje.combine(originalUtbetalingTidslinje, UtledTilkjentYtelseEndring::finnMottakereMedEndring, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        var nøkler = finnAlleUnikeNøklerMedEndring(endringstidslinje);
        return nøkler.stream().map(n -> new EndringerForMottaker(n, finnEndringstidslinjeForNøkkel(n, endringstidslinje))).toList();
    }

    private static Set<MottakerNøkkel> finnAlleUnikeNøklerMedEndring(LocalDateTimeline<List<MottakerNøkkel>> mottakereMedEndringstidslinje) {
        return mottakereMedEndringstidslinje.stream()
            .flatMap(s -> s.getValue().stream())
            .collect(Collectors.toSet());
    }

    private static LocalDateTimeline<Boolean> finnEndringstidslinjeForNøkkel(MottakerNøkkel nøkkel, LocalDateTimeline<List<MottakerNøkkel>> endringstidslinje) {
        return endringstidslinje
            .filterValue(value -> harEndringForNøkkel(nøkkel, value))
            .mapValue(it -> true)
            .compress();
    }

    private static boolean harEndringForNøkkel(MottakerNøkkel nøkkel, List<MottakerNøkkel> value) {
        return value.stream().anyMatch(endring -> endring.equals(nøkkel));
    }

    private static LocalDateSegment<List<MottakerNøkkel>> finnMottakereMedEndring(LocalDateInterval di,
                                                                                  LocalDateSegment<List<BeregningsresultatAndel>> gjeldende,
                                                                                  LocalDateSegment<List<BeregningsresultatAndel>> forrige) {
        if (gjeldende != null && forrige != null) {
            var gjeldendeVerdi = gjeldende.getValue();
            var forrigeVerdi = forrige.getValue();
            return new LocalDateSegment<>(di, finnMottakereMedEndringForPeriode(gjeldendeVerdi, forrigeVerdi));
        }

        if (gjeldende != null) {
            return new LocalDateSegment<>(di, finnMottakereMedEndringForNyPeriode(gjeldende.getValue()));
        }

        // Er det mulig å ende opp her? dvs at vi har fjernet en periode i tilkjent ytelse
        return null;

    }

    private static List<MottakerNøkkel> finnMottakereMedEndringForPeriode(List<BeregningsresultatAndel> gjeldendeAndelsliste, List<BeregningsresultatAndel> forrigeAndelsliste) {
        return finnMottakereMedEndring(gjeldendeAndelsliste, Optional.of(forrigeAndelsliste));
    }

    private static List<MottakerNøkkel> finnMottakereMedEndringForNyPeriode(List<BeregningsresultatAndel> gjeldendeAndelsliste) {
        return finnMottakereMedEndring(gjeldendeAndelsliste, Optional.empty());
    }

    private static List<MottakerNøkkel> finnMottakereMedEndring(List<BeregningsresultatAndel> gjeldendeAndelsliste, Optional<List<BeregningsresultatAndel>> forrigeAndelsliste) {
        var andelsEndringer = finnAndelsendringerFraGjeldende(gjeldendeAndelsliste, forrigeAndelsliste);
        var fjernetAndeler = forrigeAndelsliste.map(v -> finnFjernedeAndeler(gjeldendeAndelsliste, v)).orElse(Collections.emptyList());
        andelsEndringer.addAll(fjernetAndeler);
        return andelsEndringer;
    }

    private static List<MottakerNøkkel> finnFjernedeAndeler(List<BeregningsresultatAndel> gjeldendeAndelsliste, List<BeregningsresultatAndel> forrigeAndelsliste) {
        return forrigeAndelsliste.stream().filter(a ->
                gjeldendeAndelsliste.stream().noneMatch(a2 -> a2.getAktivitetsnøkkel().equals(a.getAktivitetsnøkkel())))
            .map(UtledTilkjentYtelseEndring::lagMottakerNøkkel)
            .toList();
    }

    private static MottakerNøkkel lagMottakerNøkkel(BeregningsresultatAndel a) {
        return new MottakerNøkkel(a.erBrukerMottaker(), new Aktivitetsnøkkel(a.getArbeidsgiver().orElse(null), a.getArbeidsforholdRef(), a.getAktivitetStatus(), a.getInntektskategori()));
    }


    private static List<MottakerNøkkel> finnAndelsendringerFraGjeldende(List<BeregningsresultatAndel> gjeldendeAndelsliste, Optional<List<BeregningsresultatAndel>> forrigeAndelsliste) {
        var andelsEndringer = new ArrayList<MottakerNøkkel>();

        gjeldendeAndelsliste.forEach(a -> {
            var aktivitetsnøkkel = a.getAktivitetsnøkkel();

            var matchFraForrige = forrigeAndelsliste.stream().flatMap(v -> v.stream().filter(a2 ->
                    a2.getAktivitetsnøkkel().equals(aktivitetsnøkkel)
                        && a2.erBrukerMottaker() == a.erBrukerMottaker()))
                .findFirst();

            matchFraForrige.ifPresentOrElse(a2 -> {
                    var endring = finnMottakereMedEndring(a, a2);
                    endring.ifPresent(andelsEndringer::add);
                },
                () -> andelsEndringer.add(lagMottakerNøkkel(a))
            );

        });
        return andelsEndringer;
    }


    private static Optional<MottakerNøkkel> finnMottakereMedEndring(BeregningsresultatAndel a, BeregningsresultatAndel a2) {
        if (erEndret(a, a2)) {
            return Optional.of(lagMottakerNøkkel(a));
        }

        return Optional.empty();
    }

    private static boolean erEndret(BeregningsresultatAndel a, BeregningsresultatAndel a2) {
        var endringDagsats = a.getDagsats() - a2.getDagsats();
        var endringUtbetalingsgrad = a.getUtbetalingsgrad().subtract(a2.getUtbetalingsgrad());
        var endringFeriepenger = a.getFeriepengerÅrsbeløp().getVerdi().subtract(a2.getFeriepengerÅrsbeløp().getVerdi());
        return endringDagsats != 0 || endringUtbetalingsgrad.compareTo(BigDecimal.ZERO) != 0 || endringFeriepenger.compareTo(BigDecimal.ZERO) != 0;
    }

    private static LocalDateTimeline<List<BeregningsresultatAndel>> lagResultatTidslinje(BeregningsresultatEntitet beregningsresultatEntitet) {
        var segmenter = beregningsresultatEntitet
            .getBeregningsresultatPerioder()
            .stream()
            .map(p -> new LocalDateSegment<>(p.getPeriode().toLocalDateInterval(), p.getBeregningsresultatAndelList())).toList();

        return new LocalDateTimeline<>(segmenter);
    }

    public record MottakerNøkkel(Boolean brukerErMottaker,
                                 Aktivitetsnøkkel aktivitetsnøkkel) {
    }

    public record EndringerForMottaker(
        MottakerNøkkel nøkkel,
        LocalDateTimeline<Boolean> tidslinjeMedEndringIYtelse
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
