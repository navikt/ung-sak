package no.nav.folketrygdloven.beregningsgrunnlag.modell;


import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

public class BeregningsgrunnlagArbeidstakerAndel {

    private BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel;
    private Boolean mottarYtelse;

    public static BeregningsgrunnlagArbeidstakerAndel.Builder builder() {
        return new BeregningsgrunnlagArbeidstakerAndel.Builder();
    }

    public static BeregningsgrunnlagArbeidstakerAndel.Builder builder(BeregningsgrunnlagArbeidstakerAndel eksisterendeBGArbeidstakerAndel) {
        return new BeregningsgrunnlagArbeidstakerAndel.Builder(eksisterendeBGArbeidstakerAndel);
    }

    public BeregningsgrunnlagPrStatusOgAndel getBeregningsgrunnlagPrStatusOgAndel() {
        return beregningsgrunnlagPrStatusOgAndel;
    }

    public Boolean getMottarYtelse() {
        return mottarYtelse;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagArbeidstakerAndel)) {
            return false;
        }
        BeregningsgrunnlagArbeidstakerAndel other = (BeregningsgrunnlagArbeidstakerAndel) obj;
        return Objects.equals(this.getMottarYtelse(), other.getMottarYtelse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mottarYtelse);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" //$NON-NLS-1$
                + "mottarYtelse=" + mottarYtelse + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$
    }

    public static class Builder {
        private BeregningsgrunnlagArbeidstakerAndel beregningsgrunnlagArbeidstakerAndelMal;

        public Builder() {
            beregningsgrunnlagArbeidstakerAndelMal = new BeregningsgrunnlagArbeidstakerAndel();
        }

        public Builder(BeregningsgrunnlagArbeidstakerAndel eksisterendeBGArbeidstakerAndelMal) {
            beregningsgrunnlagArbeidstakerAndelMal = eksisterendeBGArbeidstakerAndelMal;
        }

        public BeregningsgrunnlagArbeidstakerAndel.Builder medMottarYtelse(Boolean mottarYtelse) {
            beregningsgrunnlagArbeidstakerAndelMal.mottarYtelse = mottarYtelse;
            return this;
        }

        public BeregningsgrunnlagArbeidstakerAndel build(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            beregningsgrunnlagArbeidstakerAndelMal.beregningsgrunnlagPrStatusOgAndel = beregningsgrunnlagPrStatusOgAndel;
            verifyStateForBuild(beregningsgrunnlagPrStatusOgAndel);
            return beregningsgrunnlagArbeidstakerAndelMal;
        }

        public void verifyStateForBuild(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            if (!beregningsgrunnlagPrStatusOgAndel.getAktivitetStatus().erArbeidstaker()) {
                throw new IllegalArgumentException("Andel med arbeidstakerfelt må ha aktivitetstatus arbeidstaker");
            }
        }
    }
}

