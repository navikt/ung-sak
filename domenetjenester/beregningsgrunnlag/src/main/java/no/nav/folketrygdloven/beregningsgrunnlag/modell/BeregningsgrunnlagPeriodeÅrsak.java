package no.nav.folketrygdloven.beregningsgrunnlag.modell;


import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak;

public class BeregningsgrunnlagPeriodeÅrsak {

    @JsonBackReference
    private BeregningsgrunnlagPeriode beregningsgrunnlagPeriode;
    private PeriodeÅrsak periodeÅrsak = PeriodeÅrsak.UDEFINERT;

    public BeregningsgrunnlagPeriode getBeregningsgrunnlagPeriode() {
        return beregningsgrunnlagPeriode;
    }

    public PeriodeÅrsak getPeriodeÅrsak() {
        return periodeÅrsak;
    }


    public void setBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        this.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsgrunnlagPeriode, periodeÅrsak);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagPeriodeÅrsak)) {
            return false;
        }
        BeregningsgrunnlagPeriodeÅrsak other = (BeregningsgrunnlagPeriodeÅrsak) obj;
        return Objects.equals(this.getBeregningsgrunnlagPeriode(), other.getBeregningsgrunnlagPeriode())
                && Objects.equals(this.getPeriodeÅrsak(), other.getPeriodeÅrsak());
    }

    public static class Builder {
        private BeregningsgrunnlagPeriodeÅrsak beregningsgrunnlagPeriodeÅrsakMal;

        public Builder() {
            beregningsgrunnlagPeriodeÅrsakMal = new BeregningsgrunnlagPeriodeÅrsak();
        }

        public Builder medPeriodeÅrsak(PeriodeÅrsak periodeÅrsak) {
            beregningsgrunnlagPeriodeÅrsakMal.periodeÅrsak = periodeÅrsak;
            return this;
        }

        public BeregningsgrunnlagPeriodeÅrsak build(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            beregningsgrunnlagPeriodeÅrsakMal.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
            beregningsgrunnlagPeriode.addBeregningsgrunnlagPeriodeÅrsak(beregningsgrunnlagPeriodeÅrsakMal);
            return beregningsgrunnlagPeriodeÅrsakMal;
        }
    }
}
