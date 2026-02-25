package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.PgiKalkulator.avgrensOgOppjusterÅrsinntekter;


public class BeregningTjeneste {

    public static BesteberegningResultat avgjørBesteberegning(LocalDate virkningsdato, Year sistLignedeÅr, List<Inntektspost> inntekter) {
        return avgjørBesteberegning(lagBeregningInput(virkningsdato, sistLignedeÅr, inntekter));
    }

    public static BeregningInput lagBeregningInput(LocalDate virkningsdato, Year sistLignedeÅr, List<Inntektspost> inntekter) {
        var gsnittTidsserie = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();
        var inflasjonsfaktorTidsserie = GrunnbeløpTidslinje.lagInflasjonsfaktorTidslinje(Year.of(virkningsdato.getYear()), 3);
        var årsinntektMap = lagÅrsinntektMap(sistLignedeÅr, inntekter);

        return new BeregningInput(virkningsdato, sistLignedeÅr, årsinntektMap, inflasjonsfaktorTidsserie, gsnittTidsserie);
    }

    private static LocalDateTimeline<Beløp> lagÅrsinntektMap(Year sisteTilgjengeligeLigningsår, List<Inntektspost> inntekter) {
        var sisteTilgjengeligeLigningsårTom = sisteTilgjengeligeLigningsår.atMonth(12).atEndOfMonth();

        var inntektssegmenter = inntekter.stream()
            .filter(ip -> !ip.getPeriode().getTomDato().isAfter(sisteTilgjengeligeLigningsårTom))
            .map(it -> {
                var periode = it.getPeriode();

                return new LocalDateSegment<>(
                    new LocalDateInterval(periode.getFomDato(), periode.getTomDato()),
                    it.getBeløp()
                );
            }).toList();

        return new LocalDateTimeline<>(inntektssegmenter);
    }

    public static BesteberegningResultat avgjørBesteberegning(BeregningInput input) {
        var pgiPerÅr = avgrensOgOppjusterÅrsinntekter(input);

        BigDecimal årsinntektSisteÅr = pgiPerÅr.getOrDefault(input.sistLignedeÅr(), BigDecimal.ZERO);
        BigDecimal årsinntektSisteTreÅr = hentSnittTreSisteÅr(pgiPerÅr);
        BigDecimal årsinntektBesteBeregning = årsinntektSisteÅr.max(årsinntektSisteTreÅr);

        String regelSporing = LagRegelSporing.lagRegelSporingFraTidslinjer(lagRegelSporingMap(input));
        String regelInput = lagRegelInput(input);

        return new BesteberegningResultat(input, årsinntektSisteÅr, årsinntektSisteTreÅr, årsinntektBesteBeregning, regelSporing, regelInput);
    }

    private static Map<String, LocalDateTimeline<?>> lagRegelSporingMap(BeregningInput input) {
        var map = new LinkedHashMap<String, LocalDateTimeline<?>>();
        map.put("gsnittTidsserie", input.gsnittTidsserie().mapValue(Grunnbeløp::verdi));
        map.put("inflasjonsfaktorTidsserie", input.inflasjonsfaktorTidsserie());
        return map;
    }

    private static String lagRegelInput(BeregningInput input) {
        var regelInput = new RegelInput(input.virkningsdato(), input.årsinntektMap());
        return JsonObjectMapper.toJson(regelInput, LagRegelSporing.JsonMappingFeil.FACTORY::jsonMappingFeil);
    }

    private static BigDecimal hentSnittTreSisteÅr(Map<Year, BigDecimal> pgiPerÅr) {
        return pgiPerÅr.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .limit(3)
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(3), 10, java.math.RoundingMode.HALF_EVEN);
    }

    public record RegelInput(
        LocalDate virkningsdato,
        LocalDateTimeline<Beløp> inntekterPerÅr
    ) {}
}
