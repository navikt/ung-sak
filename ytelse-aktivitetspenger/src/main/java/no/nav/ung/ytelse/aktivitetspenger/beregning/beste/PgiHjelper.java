package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.typer.Beløp;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PgiHjelper {

    private final Map<Year, Map<InntektspostType, Beløp>> summerBeløpPrInntektsposttypePrÅr;
    private final Year sisteTilgjengeligeLigningsår;

    public PgiHjelper(List<Inntektspost> inntekter, Year sisteTilgjengeligeLigningsår) {
        this.sisteTilgjengeligeLigningsår = sisteTilgjengeligeLigningsår;
        var sisteTilgjengeligeLigningsårTom = sisteTilgjengeligeLigningsår.atMonth(12).atEndOfMonth();

        this.summerBeløpPrInntektsposttypePrÅr = inntekter.stream()
            .filter(ip -> !ip.getPeriode().getTomDato().isAfter(sisteTilgjengeligeLigningsårTom))
            .collect(Collectors.groupingBy(
                ip -> Year.from(ip.getPeriode().getFomDato()),
                Collectors.groupingBy(
                    Inntektspost::getInntektspostType,
                    Collectors.reducing(Beløp.ZERO, Inntektspost::getBeløp, Beløp::adder)
                )
            ));
    }

    public Map<Year, Beløp> hentPGIPrÅr() {
        return summerBeløpPrInntektsposttypePrÅr.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().values().stream().reduce(Beløp.ZERO, Beløp::adder)
            ));
    }

    public Beløp getPgi1() {
        return hentPGIPrÅr().getOrDefault(sisteTilgjengeligeLigningsår.minusYears(2), Beløp.ZERO);
    }

    public Beløp getPgi2() {
        return hentPGIPrÅr().getOrDefault(sisteTilgjengeligeLigningsår.minusYears(1), Beløp.ZERO);
    }

    public Beløp getPgi3() {
        return hentPGIPrÅr().getOrDefault(sisteTilgjengeligeLigningsår, Beløp.ZERO);
    }

    public Map<Year, Map<InntektspostType, Beløp>> getSummerBeløpPrInntektsposttypePrÅr() {
        return summerBeløpPrInntektsposttypePrÅr;
    }
}
