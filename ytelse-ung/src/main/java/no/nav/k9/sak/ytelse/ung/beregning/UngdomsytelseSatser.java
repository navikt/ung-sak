package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import no.nav.k9.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;


public record UngdomsytelseSatser(BigDecimal dagsats,
                                  BigDecimal grunnbeløp,
                                  BigDecimal grunnbeløpFaktor,
                                  UngdomsytelseSatsType satsType,
                                  Integer antallBarn,
                                  BigDecimal dagsatsBarnetillegg) {

    public static UngdomsytelseSatser.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        public static final int VIRKEDAGER_I_ET_ÅR = 260;

        private BigDecimal grunnbeløp;
        private BigDecimal grunnbeløpFaktor;
        private UngdomsytelseSatsType satsType;
        private Integer antallBarn;
        private BigDecimal dagsatsBarnetillegg;

        public Builder() {}


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

        public Builder medBarnetilleggDagsats(BigDecimal barnetilleggDagsats) {
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
    public String toString() {
        return "UngdomsytelseSatser{" +
            "dagsats=" + dagsats +
            ", grunnbeløp=" + grunnbeløp +
            ", grunnbeløpFaktor=" + grunnbeløpFaktor +
            ", satsType=" + satsType +
            '}';
    }
}
