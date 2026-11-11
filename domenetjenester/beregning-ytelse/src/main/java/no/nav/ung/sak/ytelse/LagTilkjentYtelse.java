package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;

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
    private final FagsakYtelseType ytelseType;

    public LagTilkjentYtelse(InntektsreduksjonKonfigurasjon inntektsreduksjonKonfigurasjon, FagsakYtelseType ytelseType) {
        this.inntektsreduksjonKonfigurasjon = inntektsreduksjonKonfigurasjon;
        this.ytelseType = ytelseType;
    }

    public LocalDateTimeline<TilkjentYtelsePeriodeResultat> lagTidslinje(LocalDateTimeline<YearMonth> månedsvisYtelseTidslinje,
                                                                                LocalDateTimeline<Boolean> godkjentTidslinje,
                                                                                LocalDateTimeline<BeregnetSats> totalsatsTidslinje,
                                                                                LocalDateTimeline<KontrollerteInntekter> kontrollerteInntekterTidslinje) {
        if (godkjentTidslinje.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        final var ikkePåkrevdKontrollTidslinje = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(månedsvisYtelseTidslinje, ytelseType);


        final var førstePerioder = ikkePåkrevdKontrollTidslinje.filterValue(RelevanteKontrollperioderUtleder.FritattForKontroll::gjelderFørstePeriode).mapValue(it -> true);

        // Begrenser tilkjent ytelse til periode med kontrollert inntekt eller første/siste periode
        var tidslinjeSomSkalHaTilkjentYtelse = kontrollerteInntekterTidslinje
            .intersection(godkjentTidslinje).mapValue(it -> true)
            .crossJoin(førstePerioder);

        final var sistePerioderSomSkalUtbetales = finnSistePerioderSomSkalLeggesTil(ikkePåkrevdKontrollTidslinje, tidslinjeSomSkalHaTilkjentYtelse);
        tidslinjeSomSkalHaTilkjentYtelse = tidslinjeSomSkalHaTilkjentYtelse.crossJoin(sistePerioderSomSkalUtbetales);


        return totalsatsTidslinje.combine(kontrollerteInntekterTidslinje, (di, sats, inntekter) -> {
                final var inntekt = inntekter == null ? INGEN_INNTEKT : inntekter.getValue();
                final var beregner = new ReduksjonBeregner(inntekt, inntektsreduksjonKonfigurasjon, di);
                final var periodeResultat = TilkjentYtelseBeregner.beregn(di, sats.getValue(), beregner);
                return new LocalDateSegment<>(di.getFomDato(), di.getTomDato(), periodeResultat);
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .intersection(tidslinjeSomSkalHaTilkjentYtelse);
    }

    /** Perioder som ikke slutter ved månedsslutt legges kun til dersom foregående periode er ferdig kontrollert/behandlet
     * @param ikkePåkrevdKontrollTidslinje Tidslinje der vi ikke trenger å kontrollere inntekt
     * @param tidslinjeSomSkalHaTilkjentYtelse Tidslinje der vi har utført kontroll eller skal utbetale fordi det er første periode
     * @return Tidslinje der vi skal utbetale for siste periode
     */
    private static LocalDateTimeline<Boolean> finnSistePerioderSomSkalLeggesTil(LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> ikkePåkrevdKontrollTidslinje,
                                                                                LocalDateTimeline<Boolean> tidslinjeSomSkalHaTilkjentYtelse) {
        final var segmenter = ikkePåkrevdKontrollTidslinje.filterValue(RelevanteKontrollperioderUtleder.FritattForKontroll::gjelderSistePeriode)
            .stream()
            .filter(it -> !tidslinjeSomSkalHaTilkjentYtelse.intersection(dagenFør(it)).isEmpty())
            .toList();
        return new LocalDateTimeline<>(segmenter).mapValue(it -> true);
    }

    private static LocalDateInterval dagenFør(LocalDateSegment<RelevanteKontrollperioderUtleder.FritattForKontroll> it) {
        return new LocalDateInterval(it.getFom().minusDays(1), it.getFom().minusDays(1));
    }

}
