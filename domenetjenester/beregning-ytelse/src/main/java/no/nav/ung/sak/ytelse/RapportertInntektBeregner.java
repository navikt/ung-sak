package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.domene.typer.tid.Virkedager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

public class RapportertInntektBeregner {

    private final InntektsreduksjonKonfigurasjon konfigurasjon;
    private final KontrollerteInntekter kontrollerteInntekter;
    private final BigDecimal antallVirkedager;
    private final BigDecimal antallVirkedagerHeleMåned;
    private final BigDecimal andelVirkedagerIMåned;

    public RapportertInntektBeregner(KontrollerteInntekter kontrollerteInntekter, InntektsreduksjonKonfigurasjon konfigurasjon, LocalDateInterval periode) {
        this.konfigurasjon = konfigurasjon;
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

    public record Resultat(BigDecimal reduksjon, Map<String, String> sporing) {}

    public Resultat beregnReduksjon() {
        final var sporing = new HashMap<String, String>();
        sporing.put("antallVirkedager", antallVirkedager.toString());
        sporing.put("antallVirkedagerHeleMåned", antallVirkedagerHeleMåned.toString());
        sporing.put("andelVirkedagerInnenforPeriode", andelVirkedagerIMåned.toString());

        sporing.put("rapportertArbeidsinntekt", kontrollerteInntekter.arbeidsinntekt().toString());
        sporing.put("rapportertYtelse", kontrollerteInntekter.ytelse().toString());

        sporing.put("reduksjonsfaktorArbeidsinntekt", konfigurasjon.reduksjonsfaktorArbeidsinntekt().toString());
        sporing.put("reduksjonsfaktorYtelse", konfigurasjon.reduksjonsfaktorYtelse().toString());

        var andelRapportertArbeidsInntektInnenforPeriode = kontrollerteInntekter.arbeidsinntekt().multiply(andelVirkedagerIMåned);
        sporing.put("andelRapportertInntektInnenforPeriode", andelRapportertArbeidsInntektInnenforPeriode.toString());

        var reduksjonArbeidsInntekt = andelRapportertArbeidsInntektInnenforPeriode.multiply(konfigurasjon.reduksjonsfaktorArbeidsinntekt());
        sporing.put("reduksjon", reduksjonArbeidsInntekt.toString());

        var andelRapportertYtelseInnenforPeriode = kontrollerteInntekter.ytelse().multiply(andelVirkedagerIMåned);
        sporing.put("andelRapportertYtelseInnenforPeriode", andelRapportertYtelseInnenforPeriode.toString());

        var reduksjonYtelse = andelRapportertYtelseInnenforPeriode.multiply(konfigurasjon.reduksjonsfaktorYtelse());
        sporing.put("reduksjonYtelse", reduksjonYtelse.toString());

        return new Resultat(
            reduksjonArbeidsInntekt.add(reduksjonYtelse),
            sporing
        );
    }
}
