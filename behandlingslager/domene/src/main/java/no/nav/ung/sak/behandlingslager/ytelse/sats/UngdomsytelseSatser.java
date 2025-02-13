package no.nav.ung.sak.behandlingslager.ytelse.sats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.sporing.Sporingsverdi;


public final class UngdomsytelseSatser extends Sporingsverdi {
    private final BigDecimal dagsats;
    private final BigDecimal grunnbeløp;
    private final BigDecimal grunnbeløpFaktor;
    private final UngdomsytelseSatsType satsType;
    private final Integer antallBarn;
    private final int dagsatsBarnetillegg;

    public UngdomsytelseSatser(BigDecimal dagsats,
                               BigDecimal grunnbeløp,
                               BigDecimal grunnbeløpFaktor,
                               UngdomsytelseSatsType satsType,
                               Integer antallBarn,
                               int dagsatsBarnetillegg) {
        this.dagsats = dagsats;
        this.grunnbeløp = grunnbeløp;
        this.grunnbeløpFaktor = grunnbeløpFaktor;
        this.satsType = satsType;
        this.antallBarn = antallBarn;
        this.dagsatsBarnetillegg = dagsatsBarnetillegg;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        public static final int VIRKEDAGER_I_ET_ÅR = 260;

        private BigDecimal grunnbeløp;
        private BigDecimal grunnbeløpFaktor;
        private UngdomsytelseSatsType satsType;
        private Integer antallBarn;
        private int dagsatsBarnetillegg;

        public Builder() {
        }

        private Builder(Builder builder) {
            grunnbeløp = builder.grunnbeløp;
            grunnbeløpFaktor = builder.grunnbeløpFaktor;
            satsType = builder.satsType;
            antallBarn = builder.antallBarn;
            dagsatsBarnetillegg = builder.dagsatsBarnetillegg;
        }

        public Builder kopi() {
            return new Builder(this);
        }

        public Builder medGrunnbeløp(BigDecimal grunnbeløp) {
            this.grunnbeløp = grunnbeløp;
            return this;
        }

        public Builder medGrunnbeløpFaktor(BigDecimal grunnbeløpFaktor) {
            this.grunnbeløpFaktor = grunnbeløpFaktor;
            return this;
        }

        public Builder medSatstype(UngdomsytelseSatsType ungdomsytelseSatsType) {
            this.satsType = ungdomsytelseSatsType;
            return this;
        }

        public Builder medAntallBarn(Integer antallBarn) {
            this.antallBarn = antallBarn;
            return this;
        }

        public Builder medBarnetilleggDagsats(int barnetilleggDagsats) {
            this.dagsatsBarnetillegg = barnetilleggDagsats;
            return this;
        }

        public UngdomsytelseSatser build() {
            BigDecimal dagsats = grunnbeløpFaktor.multiply(grunnbeløp)
                .divide(BigDecimal.valueOf(VIRKEDAGER_I_ET_ÅR), 2, RoundingMode.HALF_UP);
            Objects.requireNonNull(antallBarn);
            Objects.requireNonNull(dagsatsBarnetillegg);
            Objects.requireNonNull(satsType);
            return new UngdomsytelseSatser(dagsats, grunnbeløp, grunnbeløpFaktor, satsType, antallBarn, dagsatsBarnetillegg);
        }


    }

    @Override
    public String tilRegelVerdi() {
        return toString();
    }

    @Override
    public String toString() {
        return "UngdomsytelseSatser{" +
            "dagsats=" + dagsats +
            ", grunnbeløp=" + grunnbeløp +
            ", grunnbeløpFaktor=" + grunnbeløpFaktor +
            ", satsType=" + satsType +
            '}';
    }

    public BigDecimal dagsats() {
        return dagsats;
    }

    public BigDecimal grunnbeløp() {
        return grunnbeløp;
    }

    public BigDecimal grunnbeløpFaktor() {
        return grunnbeløpFaktor;
    }

    public UngdomsytelseSatsType satsType() {
        return satsType;
    }

    public Integer antallBarn() {
        return antallBarn;
    }

    public int dagsatsBarnetillegg() {
        return dagsatsBarnetillegg;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (UngdomsytelseSatser) obj;
        return Objects.equals(this.dagsats, that.dagsats) &&
            Objects.equals(this.grunnbeløp, that.grunnbeløp) &&
            Objects.equals(this.grunnbeløpFaktor, that.grunnbeløpFaktor) &&
            Objects.equals(this.satsType, that.satsType) &&
            Objects.equals(this.antallBarn, that.antallBarn) &&
            this.dagsatsBarnetillegg == that.dagsatsBarnetillegg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dagsats, grunnbeløp, grunnbeløpFaktor, satsType, antallBarn, dagsatsBarnetillegg);
    }

}
