package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.FinnGjennomsnittligPGI.finnGjennomsnittligPGI;

public class BesteBeregning {

    public record BesteBeregningInput(
        LocalDate virkningsdato,
        Map<Year, BigDecimal> årsinntektMap,
        LocalDateTimeline<BigDecimal> inflasjonsfaktorTidsserie,
        LocalDateTimeline<Grunnbeløp> gsnittTidsserie
    ) {}

    private final LocalDate virkningsdato;

    public BesteBeregning(LocalDate virkningsdato) {
        this.virkningsdato = virkningsdato;
    }

    public BesteBeregningResultat avgjørBestePGI(List<Inntektspost> inntektsposter) {
        var sistLignedeÅr = Year.of(virkningsdato.minusYears(1).getYear());  // TODO: Koble på utledning av siste tilgjengelige lignede år

        var input = lagBesteBeregningInput(virkningsdato, sistLignedeÅr, inntektsposter);
        var pgiPerÅr = finnGjennomsnittligPGI(input);

        BigDecimal årsinntektSisteÅr = pgiPerÅr.getOrDefault(sistLignedeÅr, BigDecimal.ZERO);
        BigDecimal årsinntektSisteTreÅr = hentSnittTreSisteÅr(pgiPerÅr);
        BigDecimal årsinntektBesteBeregning = årsinntektSisteÅr.max(årsinntektSisteTreÅr);

        String regelSporing = LagRegelSporing.lagRegelSporingFraTidslinjer(lagRegelSporingMap(input));
        String regelInput = lagRegelInput(input);

        return new BesteBeregningResultat(virkningsdato, årsinntektSisteÅr, årsinntektSisteTreÅr, årsinntektBesteBeregning, regelSporing, regelInput);
    }

    private static Map<String, LocalDateTimeline<?>> lagRegelSporingMap(BesteBeregningInput input) {
        var map = new LinkedHashMap<String, LocalDateTimeline<?>>();
        map.put("gsnittTidsserie", input.gsnittTidsserie().mapValue(Grunnbeløp::verdi));
        map.put("inflasjonsfaktorTidsserie", input.inflasjonsfaktorTidsserie());
        return map;
    }

    private static String lagRegelInput(BesteBeregningInput input) {
        var regelInput = new RegelInput(input.virkningsdato(), input.årsinntektMap());
        return JsonObjectMapper.toJson(regelInput, LagRegelSporing.JsonMappingFeil.FACTORY::jsonMappingFeil);
    }

    private BigDecimal hentSnittTreSisteÅr(Map<Year, BigDecimal> pgiPerÅr) {
        return pgiPerÅr.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .limit(3)
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(3), 10, java.math.RoundingMode.HALF_EVEN);
    }

    public record RegelInput(
        LocalDate virkningsdato,
        Map<Year, BigDecimal> inntekterPerÅr
    ) {}

    public static BesteBeregning.BesteBeregningInput lagBesteBeregningInput(LocalDate virkningsdato, Year sisteTilgjengeligeGSnittÅr, List<Inntektspost> inntekter) {
        var gsnittTidsserie = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();
        var inflasjonsfaktorTidsserie = GrunnbeløpTidslinje.lagInflasjonsfaktorTidslinje(Year.of(virkningsdato.getYear()), 3);
        var årsinntektMap = lagÅrsinntektMap(sisteTilgjengeligeGSnittÅr, inntekter);
        return new BesteBeregning.BesteBeregningInput(virkningsdato, årsinntektMap, inflasjonsfaktorTidsserie, gsnittTidsserie);
    }

    private static Map<Year, BigDecimal> lagÅrsinntektMap(Year sisteTilgjengeligeLigningsår, List<Inntektspost> inntekter) {
        var sisteTilgjengeligeLigningsårTom = sisteTilgjengeligeLigningsår.atMonth(12).atEndOfMonth();

        return inntekter.stream()
            .filter(ip -> !ip.getPeriode().getTomDato().isAfter(sisteTilgjengeligeLigningsårTom))
            .collect(Collectors.groupingBy(
                ip -> Year.of(ip.getPeriode().getFomDato().getYear()),
                Collectors.reducing(
                    BigDecimal.ZERO,
                    ip -> ip.getBeløp().getVerdi(),
                    BigDecimal::add
                )
            ));
    }
}
