package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.kontroll.InntektskontrollFritakUtleder;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * `LagTilkjentYtelse` er en hjelpeklasse som brukes til å generere en tidslinje for tilkjent ytelse basert på godkjente perioder,
 * totale satser og perioder med kontrollert inntekt.
 * <p>
 * Bare perioder med kontrollert inntekt eller ytelsesspesifikt fritak fra kontroll tas med i tilkjent ytelse.
 */
public class LagTilkjentYtelse {

    private static final KontrollerteInntekter INGEN_INNTEKT = new KontrollerteInntekter(BigDecimal.ZERO, BigDecimal.ZERO);
    private InntektsreduksjonKonfigurasjon inntektsreduksjonKonfigurasjon;
    private final InntektskontrollFritakUtleder fritakUtleder;

    public LagTilkjentYtelse(InntektsreduksjonKonfigurasjon inntektsreduksjonKonfigurasjon, InntektskontrollFritakUtleder fritakUtleder) {
        this.inntektsreduksjonKonfigurasjon = inntektsreduksjonKonfigurasjon;
        this.fritakUtleder = fritakUtleder;
    }

    public LocalDateTimeline<TilkjentYtelsePeriodeResultat> lagTidslinje(LocalDateTimeline<YearMonth> månedsvisYtelseTidslinje,
                                                                                LocalDateTimeline<Boolean> godkjentTidslinje,
                                                                                LocalDateTimeline<BeregnetSats> totalsatsTidslinje,
                                                                                LocalDateTimeline<KontrollerteInntekter> kontrollerteInntekterTidslinje) {
        if (godkjentTidslinje.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        final var ikkePåkrevdKontrollTidslinje = fritakUtleder.finnPerioderFritattForKontroll(månedsvisYtelseTidslinje);
        final var tidslinjeSomSkalHaTilkjentYtelse = kontrollerteInntekterTidslinje
            .intersection(godkjentTidslinje).mapValue(it -> true)
            .crossJoin(ikkePåkrevdKontrollTidslinje);

        return totalsatsTidslinje.combine(kontrollerteInntekterTidslinje, (di, sats, inntekter) -> {
                final var inntekt = inntekter == null ? INGEN_INNTEKT : inntekter.getValue();
                final var beregner = new ReduksjonBeregner(inntekt, inntektsreduksjonKonfigurasjon, di);
                final var periodeResultat = TilkjentYtelseBeregner.beregn(di, sats.getValue(), beregner);
                return new LocalDateSegment<>(di.getFomDato(), di.getTomDato(), periodeResultat);
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .intersection(tidslinjeSomSkalHaTilkjentYtelse);
    }

}
