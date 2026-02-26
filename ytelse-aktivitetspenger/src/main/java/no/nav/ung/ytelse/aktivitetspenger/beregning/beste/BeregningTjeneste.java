package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.PgiKalkulator.avgrensOgOppjusterÅrsinntekter;
import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.PgiKalkulator.lagPgiKalkulatorInput;


public class BeregningTjeneste {

    public static BeregningInput lagBeregningInput(Year sisteTilgjengeligeLigningsår, LocalDate virkningsdato, List<Inntektspost> inntekter) {
        var sisteTilgjengeligeLigningsårTom = sisteTilgjengeligeLigningsår.atMonth(12).atEndOfMonth();

        var pgiPerÅr = inntekter.stream()
            .filter(ip -> !ip.getPeriode().getTomDato().isAfter(sisteTilgjengeligeLigningsårTom))
            .collect(Collectors.groupingBy(
                ip -> Year.from(ip.getPeriode().getFomDato()),
                Collectors.reducing(Beløp.ZERO, Inntektspost::getBeløp, Beløp::adder)
            ));

        var pgi3 = pgiPerÅr.getOrDefault(sisteTilgjengeligeLigningsår, Beløp.ZERO);
        var pgi2 = pgiPerÅr.getOrDefault(sisteTilgjengeligeLigningsår.minusYears(1), Beløp.ZERO);
        var pgi1 = pgiPerÅr.getOrDefault(sisteTilgjengeligeLigningsår.minusYears(2), Beløp.ZERO);

        return new BeregningInput(pgi1, pgi2, pgi3, virkningsdato, sisteTilgjengeligeLigningsår);
    }

    public static BesteberegningResultat avgjørBesteberegning(BeregningInput input) {
        var pgiKalkulatorInput = lagPgiKalkulatorInput(input);
        var pgiPerÅr = avgrensOgOppjusterÅrsinntekter(pgiKalkulatorInput);

        BigDecimal årsinntektSisteÅr = pgiPerÅr.getOrDefault(input.sisteLignedeÅr(), BigDecimal.ZERO);
        BigDecimal årsinntektSisteTreÅr = hentSnittTreSisteÅr(pgiPerÅr);
        BigDecimal årsinntektBesteBeregning = årsinntektSisteÅr.max(årsinntektSisteTreÅr);

        String regelSporing = LagRegelSporing.lagRegelSporingFraTidslinjer(lagRegelSporingMap(pgiKalkulatorInput));

        return new BesteberegningResultat(input, årsinntektSisteÅr, årsinntektSisteTreÅr, årsinntektBesteBeregning, regelSporing);
    }

    private static Map<String, LocalDateTimeline<?>> lagRegelSporingMap(PgiKalkulatorInput input) {
        var map = new LinkedHashMap<String, LocalDateTimeline<?>>();
        map.put("gsnittTidsserie", input.gsnittTidsserie().mapValue(Grunnbeløp::verdi));
        map.put("inflasjonsfaktorTidsserie", input.inflasjonsfaktorTidsserie());
        return map;
    }

    private static BigDecimal hentSnittTreSisteÅr(Map<Year, BigDecimal> pgiPerÅr) {
        return pgiPerÅr.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .limit(3)
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(3), 10, java.math.RoundingMode.HALF_EVEN);
    }
}
