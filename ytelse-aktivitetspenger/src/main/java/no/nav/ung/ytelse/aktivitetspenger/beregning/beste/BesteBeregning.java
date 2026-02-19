package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.FinnGjennomsnittligPGI.finnGjennomsnittligPGI;

public class BesteBeregning {

    private final LocalDate virkningsdato;

    public BesteBeregning(LocalDate virkningsdato) {
        this.virkningsdato = virkningsdato;
    }

    public BesteBeregningResultat avgjørBestePGI(List<Inntektspost> inntektsposter) {
        var gjennomsnittUtregningResultat = finnGjennomsnittligPGI(virkningsdato, inntektsposter);
        var pgiPerÅr = gjennomsnittUtregningResultat.pgiPerÅr();

        BigDecimal sisteÅrVerdi = hentSisteÅr(pgiPerÅr);
        BigDecimal snittTreSisteÅr = hentSnittTreSisteÅr(pgiPerÅr);
        BigDecimal størsteAvSistÅrOgSnittAvTreSisteÅr = sisteÅrVerdi.max(snittTreSisteÅr);

        String regelSporing = LagRegelSporing.lagRegelSporingFraTidslinjer(gjennomsnittUtregningResultat.regelSporingMap());
        String regelInput = lagRegelInput(virkningsdato, inntektsposter);

        return new BesteBeregningResultat(sisteÅrVerdi, snittTreSisteÅr, størsteAvSistÅrOgSnittAvTreSisteÅr, regelSporing, regelInput);
    }

    private static String lagRegelInput(LocalDate virkningsdato, List<Inntektspost> inntektsposter) {
        var inntekterPerÅr = inntektsposter.stream()
            .collect(Collectors.groupingBy(
                ip -> Year.of(ip.getPeriode().getFomDato().getYear()),
                Collectors.mapping(
                    ip -> new RegelInput.MånedsinntektEntry(
                        ip.getPeriode().getFomDato(),
                        ip.getBeløp().getVerdi()
                    ),
                    Collectors.toList()
                )
            ));
        var regelInput = new RegelInput(virkningsdato, inntekterPerÅr);
        return JsonObjectMapper.toJson(regelInput, LagRegelSporing.JsonMappingFeil.FACTORY::jsonMappingFeil);
    }

    private BigDecimal hentSisteÅr(Map<Year, BigDecimal> pgiPerÅr) {
        return pgiPerÅr.entrySet().stream()
            .max(Map.Entry.comparingByKey())
            .orElseThrow()
            .getValue();
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
        Map<Year, List<MånedsinntektEntry>> inntekterPerÅr
    ) {
        public record MånedsinntektEntry(LocalDate fom, BigDecimal beløp) {}
    }
}
