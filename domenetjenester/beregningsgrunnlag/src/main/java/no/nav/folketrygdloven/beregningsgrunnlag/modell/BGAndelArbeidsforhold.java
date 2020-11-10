package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class BGAndelArbeidsforhold {

    private BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private BigDecimal refusjonskravPrÅr;
    private BigDecimal naturalytelseBortfaltPrÅr;
    private BigDecimal naturalytelseTilkommetPrÅr;
    private LocalDate arbeidsperiodeFom;
    private LocalDate arbeidsperiodeTom;

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    public BigDecimal getRefusjonskravPrÅr() {
        return refusjonskravPrÅr;
    }

    public Optional<BigDecimal> getNaturalytelseBortfaltPrÅr() {
        return Optional.ofNullable(naturalytelseBortfaltPrÅr);
    }

    public Optional<BigDecimal> getNaturalytelseTilkommetPrÅr() {
        return Optional.ofNullable(naturalytelseTilkommetPrÅr);
    }

    public LocalDate getArbeidsperiodeFom() {
        return arbeidsperiodeFom;
    }

    public Optional<LocalDate> getArbeidsperiodeTom() {
        return Optional.ofNullable(arbeidsperiodeTom);
    }

    public DatoIntervallEntitet getArbeidsperiode() {
        if (arbeidsperiodeTom == null) {
            return DatoIntervallEntitet.fraOgMed(arbeidsperiodeFom);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsperiodeFom, arbeidsperiodeTom);
    }

    public String getArbeidsforholdOrgnr() {
        return getArbeidsgiver().getOrgnr();
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BGAndelArbeidsforhold)) {
            return false;
        }
        BGAndelArbeidsforhold other = (BGAndelArbeidsforhold) obj;
        return Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver())
                && Objects.equals(this.arbeidsforholdRef, other.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArbeidsgiver(), arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" //$NON-NLS-1$
                + "orgnr=" + getArbeidsforholdOrgnr() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "arbeidsgiver=" + arbeidsgiver + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "arbeidsforholdRef=" + arbeidsforholdRef + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "naturalytelseBortfaltPrÅr=" + naturalytelseBortfaltPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "naturalytelseTilkommetPrÅr=" + naturalytelseTilkommetPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "refusjonskravPrÅr=" + refusjonskravPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "arbeidsperiodeFom=" + arbeidsperiodeFom //$NON-NLS-1$
                + "arbeidsperiodeTom=" + arbeidsperiodeTom //$NON-NLS-1$
                + ">"; //$NON-NLS-1$
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        return bgAndelArbeidsforhold == null ? new Builder() : new Builder(bgAndelArbeidsforhold);
    }

    public static class Builder {
        private BGAndelArbeidsforhold bgAndelArbeidsforhold;

        private Builder() {
            bgAndelArbeidsforhold = new BGAndelArbeidsforhold();
        }

        private Builder(BGAndelArbeidsforhold eksisterendeBGAndelArbeidsforhold) {
            bgAndelArbeidsforhold = eksisterendeBGAndelArbeidsforhold;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            bgAndelArbeidsforhold.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdRef(String arbeidsforholdRef) {
            return medArbeidsforholdRef(arbeidsforholdRef==null?null:InternArbeidsforholdRef.ref(arbeidsforholdRef));
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            bgAndelArbeidsforhold.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medNaturalytelseBortfaltPrÅr(BigDecimal naturalytelseBortfaltPrÅr) {
            bgAndelArbeidsforhold.naturalytelseBortfaltPrÅr = naturalytelseBortfaltPrÅr;
            return this;
        }

        public Builder medNaturalytelseTilkommetPrÅr(BigDecimal naturalytelseTilkommetPrÅr) {
            bgAndelArbeidsforhold.naturalytelseTilkommetPrÅr = naturalytelseTilkommetPrÅr;
            return this;
        }

        public Builder medRefusjonskravPrÅr(BigDecimal refusjonskravPrÅr) {
            bgAndelArbeidsforhold.refusjonskravPrÅr = refusjonskravPrÅr;
            return this;
        }

        public Builder medArbeidsperiodeFom(LocalDate arbeidsperiodeFom) {
            bgAndelArbeidsforhold.arbeidsperiodeFom = arbeidsperiodeFom;
            return this;
        }

        public Builder medArbeidsperiodeTom(LocalDate arbeidsperiodeTom) {
            bgAndelArbeidsforhold.arbeidsperiodeTom = arbeidsperiodeTom;
            return this;
        }

        BGAndelArbeidsforhold build(BeregningsgrunnlagPrStatusOgAndel andel) {
            Objects.requireNonNull(bgAndelArbeidsforhold.arbeidsgiver, "arbeidsgiver");
            bgAndelArbeidsforhold.beregningsgrunnlagPrStatusOgAndel = andel;
            return bgAndelArbeidsforhold;
        }
    }
}
