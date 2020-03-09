package no.nav.folketrygdloven.beregningsgrunnlag.modell;


import java.util.Objects;

public class BeregningsgrunnlagFrilansAndel {

    private BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel;
    private Boolean mottarYtelse;
    private Boolean nyoppstartet;

    public BeregningsgrunnlagPrStatusOgAndel getBeregningsgrunnlagPrStatusOgAndel() {
        return beregningsgrunnlagPrStatusOgAndel;
    }

    public Boolean getMottarYtelse() {
        return mottarYtelse;
    }

    public Boolean getNyoppstartet() {
        return nyoppstartet;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagFrilansAndel)) {
            return false;
        }
        BeregningsgrunnlagFrilansAndel other = (BeregningsgrunnlagFrilansAndel) obj;
        return Objects.equals(this.getMottarYtelse(), other.getMottarYtelse())
                && Objects.equals(this.getNyoppstartet(), other.getNyoppstartet());
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


    public static BeregningsgrunnlagFrilansAndel.Builder builder() {
        return new BeregningsgrunnlagFrilansAndel.Builder();
    }

    public static BeregningsgrunnlagFrilansAndel.Builder builder(BeregningsgrunnlagFrilansAndel eksisterendeBGFrilansAndel) {
        return new BeregningsgrunnlagFrilansAndel.Builder(eksisterendeBGFrilansAndel);
    }

    public static class Builder {
        private BeregningsgrunnlagFrilansAndel beregningsgrunnlagFrilansAndelMal;

        public Builder() {
            beregningsgrunnlagFrilansAndelMal = new BeregningsgrunnlagFrilansAndel();
        }

        public Builder(BeregningsgrunnlagFrilansAndel eksisterendeBGFrilansAndelMal) {
            beregningsgrunnlagFrilansAndelMal = eksisterendeBGFrilansAndelMal;
        }

        BeregningsgrunnlagFrilansAndel.Builder medMottarYtelse(Boolean mottarYtelse) {
            beregningsgrunnlagFrilansAndelMal.mottarYtelse = mottarYtelse;
            return this;
        }

        public BeregningsgrunnlagFrilansAndel.Builder medNyoppstartet(Boolean nyoppstartet) {
            beregningsgrunnlagFrilansAndelMal.nyoppstartet = nyoppstartet;
            return this;
        }

        public BeregningsgrunnlagFrilansAndel build(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            beregningsgrunnlagFrilansAndelMal.beregningsgrunnlagPrStatusOgAndel = beregningsgrunnlagPrStatusOgAndel;
            verifyStateForBuild(beregningsgrunnlagPrStatusOgAndel);
            return beregningsgrunnlagFrilansAndelMal;
        }

        public void verifyStateForBuild(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            if (!beregningsgrunnlagPrStatusOgAndel.getAktivitetStatus().erFrilanser()) {
                throw new IllegalArgumentException("Andel med frilansfelt må ha aktivitetstatus frilans");
            }
        }
    }
}
