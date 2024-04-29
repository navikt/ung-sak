package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

public class UttakAktivitet {
    private BigDecimal stillingsgrad;
    private BigDecimal utbetalingsgrad;
    private Arbeidsforhold arbeidsforhold;
    private UttakArbeidType type;
    private Map<Vilkår, Utfall> vilkårPrAktivitet;

    public UttakAktivitet(BigDecimal stillingsgrad, BigDecimal utbetalingsgrad, Arbeidsforhold arbeidsforhold, UttakArbeidType type, Map<Vilkår, Utfall> vilkårPrAktivitet) {
        this.stillingsgrad = Objects.requireNonNull(stillingsgrad, "stillingsgrad");
        this.utbetalingsgrad = Objects.requireNonNull(utbetalingsgrad, "utbetalingsgrad");
        this.arbeidsforhold = arbeidsforhold;
        this.type = type;
        this.vilkårPrAktivitet = vilkårPrAktivitet;
        if (type == UttakArbeidType.FRILANSER) {
            Objects.requireNonNull(arbeidsforhold, "Krever arbeidsforhold for type " + type);
        }
        if (type == UttakArbeidType.FRILANSER && !arbeidsforhold.erFrilanser() || type != UttakArbeidType.FRILANSER && arbeidsforhold != null && arbeidsforhold.erFrilanser()) {
            throw new IllegalArgumentException("Mismatch mellom uttakarbeidstype " + type + " og arbeidsforhold.erFrilanser " + arbeidsforhold.erFrilanser());
        }
    }

    public BigDecimal getStillingsgrad() {
        return stillingsgrad;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public UttakArbeidType getType() {
        return type;
    }

    public Map<Vilkår, Utfall> getVilkårPrAktivitet() {
        return vilkårPrAktivitet;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<type=" + type + ", arbeid=" + arbeidsforhold + ", utbetalingsgrad=" + utbetalingsgrad + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var other = (UttakAktivitet) obj;
        return Objects.equals(type, other.type)
            && Objects.equals(arbeidsforhold, other.arbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, arbeidsforhold);
    }
}
