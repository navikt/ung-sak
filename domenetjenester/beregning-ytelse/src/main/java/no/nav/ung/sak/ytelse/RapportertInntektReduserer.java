package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.domene.typer.tid.Virkedager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

public class RapportertInntektReduserer {

    public static final BigDecimal REDUKSJONS_FAKTOR_ARBEIDSINNTEKT = BigDecimal.valueOf(0.66);
    public static final BigDecimal REDUKSJONS_FAKTOR_YTELSE = BigDecimal.valueOf(1.00);

    private final KontrollerteInntekter kontrollerteInntekter;
    private final BigDecimal antallVirkedager;
    private final BigDecimal antallVirkedagerHeleMåned;
    private final BigDecimal andelVirkedagerIMåned;

    public RapportertInntektReduserer(KontrollerteInntekter kontrollerteInntekter, LocalDateInterval periode) {
        this.kontrollerteInntekter = kontrollerteInntekter;

        this.antallVirkedager = BigDecimal.valueOf(Virkedager.beregnAntallVirkedager(periode.getFomDato(), periode.getTomDato()));
        this.antallVirkedagerHeleMåned = BigDecimal.valueOf(Virkedager.beregnAntallVirkedager(
            periode.getFomDato().withDayOfMonth(1),
            periode.getTomDato().with(TemporalAdjusters.lastDayOfMonth())));

        this.andelVirkedagerIMåned = antallVirkedager.divide(antallVirkedagerHeleMåned, 10, RoundingMode.HALF_UP);
    }

    public BigDecimal antallVirkedager() {
        return antallVirkedager;
    }

    public BigDecimal beregnReduksjon(Map<String, String> sporing) {
        sporing.put("antallVirkedager", antallVirkedager.toString());
        sporing.put("antallVirkedagerHeleMåned", antallVirkedagerHeleMåned.toString());
        sporing.put("andelVirkedagerInnenforPeriode", andelVirkedagerIMåned.toString());

        sporing.put("rapportertArbeidsinntekt", kontrollerteInntekter.arbeidsinntekt().toString());
        sporing.put("rapportertYtelse", kontrollerteInntekter.ytelse().toString());

        sporing.put("reduksjonsfaktorArbeidsinntekt", REDUKSJONS_FAKTOR_ARBEIDSINNTEKT.toString());
        sporing.put("reduksjonsfaktorYtelse", REDUKSJONS_FAKTOR_YTELSE.toString());

        var andelRapportertArbeidsInntektInnenforPeriode = kontrollerteInntekter.arbeidsinntekt().multiply(andelVirkedagerIMåned);
        sporing.put("andelRapportertInntektInnenforPeriode", andelRapportertArbeidsInntektInnenforPeriode.toString());

        var reduksjonArbeidsInntekt = andelRapportertArbeidsInntektInnenforPeriode.multiply(REDUKSJONS_FAKTOR_ARBEIDSINNTEKT);
        sporing.put("reduksjon", reduksjonArbeidsInntekt.toString());

        var andelRapportertYtelseInnenforPeriode = kontrollerteInntekter.ytelse().multiply(andelVirkedagerIMåned);
        sporing.put("andelRapportertYtelseInnenforPeriode", andelRapportertYtelseInnenforPeriode.toString());

        var reduksjonYtelse = andelRapportertYtelseInnenforPeriode.multiply(REDUKSJONS_FAKTOR_YTELSE);
        sporing.put("reduksjonYtelse", reduksjonYtelse.toString());

        return reduksjonArbeidsInntekt.add(reduksjonYtelse);
    }
}
