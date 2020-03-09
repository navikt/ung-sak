package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import java.util.Optional;

import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAktivitetHandlingType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class BeregningAktivitetOverstyring {
    private ÅpenDatoIntervallEntitet periode;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private BeregningAktivitetHandlingType handlingType;
    private OpptjeningAktivitetType opptjeningAktivitetType;
    private BeregningAktivitetOverstyringer overstyringerEntitet;
    public BeregningAktivitetHandlingType getHandling() {
        return handlingType;
    }
    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }
    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }
    public OpptjeningAktivitetType getOpptjeningAktivitetType() {
        return opptjeningAktivitetType;
    }
    public ÅpenDatoIntervallEntitet getPeriode() {
        return periode;
    }

    public BeregningAktivitetNøkkel getNøkkel() {
        return BeregningAktivitetNøkkel.builder()
                .medArbeidsgiverIdentifikator(getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null))
                .medArbeidsforholdRef(arbeidsforholdRef != null ? arbeidsforholdRef.getReferanse() : null)
                .medOpptjeningAktivitetType(opptjeningAktivitetType)
                .medFom(periode.getFomDato())
                .medTom(periode.getTomDato())
                .build();
    }

    void setBeregningAktivitetOverstyringer(BeregningAktivitetOverstyringer overstyringerEntitet) {
        this.overstyringerEntitet = overstyringerEntitet;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningAktivitetOverstyring kladd;

        private Builder() {
            kladd = new BeregningAktivitetOverstyring();
        }

        public Builder medPeriode(ÅpenDatoIntervallEntitet periode) {
            kladd.periode = periode;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            kladd.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medOpptjeningAktivitetType(OpptjeningAktivitetType opptjeningAktivitetType) {
            kladd.opptjeningAktivitetType = opptjeningAktivitetType;
            return this;
        }

        public Builder medHandling(BeregningAktivitetHandlingType beregningAktivitetHandlingType) {
            kladd.handlingType = beregningAktivitetHandlingType;
            return this;
        }

        public BeregningAktivitetOverstyring build() {
            return kladd;
        }
    }
}
