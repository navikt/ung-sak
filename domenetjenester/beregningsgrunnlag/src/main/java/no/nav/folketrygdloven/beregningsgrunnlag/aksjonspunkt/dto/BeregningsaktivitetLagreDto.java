package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetNøkkel;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class BeregningsaktivitetLagreDto {
    @NotNull
    private OpptjeningAktivitetType opptjeningAktivitetType;

    @NotNull
    private LocalDate fom;

    private LocalDate tom;

    @Pattern(regexp = "\\d{9}|\\d{13}")
    private String oppdragsgiverOrg;

    private String arbeidsgiverIdentifikator;

    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdRef;

    private boolean skalBrukes;

    BeregningsaktivitetLagreDto() { //NOSONAR
        //for jackson
    }

    public OpptjeningAktivitetType getOpptjeningAktivitetType() {
        return opptjeningAktivitetType;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getOppdragsgiverOrg() {
        return oppdragsgiverOrg;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public boolean getSkalBrukes() {
        return skalBrukes;
    }

    public BeregningAktivitetNøkkel getNøkkel() {
        BeregningAktivitetNøkkel.Builder builder = BeregningAktivitetNøkkel.builder()
            .medOpptjeningAktivitetType(opptjeningAktivitetType)
            .medFom(fom)
            .medTom(tom)
            .medArbeidsforholdRef(arbeidsforholdRef);
        if (oppdragsgiverOrg != null) {
            builder.medArbeidsgiverIdentifikator(oppdragsgiverOrg);
        } else {
            builder.medArbeidsgiverIdentifikator(arbeidsgiverIdentifikator);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsaktivitetLagreDto kladd;

        private Builder() {
            kladd = new BeregningsaktivitetLagreDto();
        }

        public Builder medOpptjeningAktivitetType(OpptjeningAktivitetType opptjeningAktivitetType) {
            kladd.opptjeningAktivitetType = opptjeningAktivitetType;
            return this;
        }

        public Builder medFom(LocalDate fom) {
            kladd.fom = fom;
            return this;
        }

        public Builder medTom(LocalDate tom) {
            kladd.tom = tom;
            return this;
        }

        public Builder medOppdragsgiverOrg(String oppdragsgiverOrg) {
            kladd.oppdragsgiverOrg = oppdragsgiverOrg;
            return this;
        }

        public Builder medArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
            kladd.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
            return this;
        }

        public Builder medArbeidsforholdRef(String arbeidsforholdRef) {
            kladd.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medSkalBrukes(boolean skalBrukes) {
            kladd.skalBrukes = skalBrukes;
            return this;
        }

        public BeregningsaktivitetLagreDto build() {
            return kladd;
        }
    }
}
