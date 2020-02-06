package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsaktivitetLagreDto {

    @JsonProperty(value = "opptjeningAktivitetType", required = true)
    @NotNull
    @Valid
    private OpptjeningAktivitetType opptjeningAktivitetType;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    private LocalDate tom;

    @JsonProperty(value = "oppdragsgiverOrg")
    @Pattern(regexp = "\\d{9}|\\d{13}")
    private String oppdragsgiverOrg;

    @JsonProperty(value = "arbeidsgiverIdentifikator")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value = "arbeidsforholdRef")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{XDigit}\\-]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdRef;

    @JsonProperty(value = "skalBrukes")
    private boolean skalBrukes;

    BeregningsaktivitetLagreDto() { // NOSONAR
        // for jackson
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
