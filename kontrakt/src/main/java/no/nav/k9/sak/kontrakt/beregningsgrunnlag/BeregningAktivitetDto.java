package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Objects;

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
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningAktivitetDto {

    @JsonProperty(value = "arbeidsgiverNavn")
    @Size(max = 300)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverNavn;

    @JsonProperty(value = "arbeidsgiverId")
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverId;

    @JsonProperty(value = "eksternArbeidsforholdId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String eksternArbeidsforholdId;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    /** For virksomheter - orgnr. For personlige arbeidsgiver - aktørId. */
    @JsonProperty(value = "arbeidsforholdId")
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdId;

    @JsonProperty(value = "arbeidsforholdType", required = true)
    @NotNull
    @Valid
    private OpptjeningAktivitetType arbeidsforholdType;

    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "aktørIdString")
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String aktørIdString;

    @JsonProperty(value = "skalBrukes")
    private Boolean skalBrukes;

    public BeregningAktivitetDto() {
        // jackson
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public void setArbeidsgiverId(String arbeidsgiverId) {
        this.arbeidsgiverId = arbeidsgiverId;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public void setArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public String getAktørIdString() {
        return aktørIdString;
    }

    public void setAktørIdString(String aktørIdString) {
        this.aktørIdString = aktørIdString;
    }

    public Boolean getSkalBrukes() {
        return skalBrukes;
    }

    public void setSkalBrukes(Boolean skalBrukes) {
        this.skalBrukes = skalBrukes;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BeregningAktivitetDto that = (BeregningAktivitetDto) o;
        return Objects.equals(arbeidsgiverId, that.arbeidsgiverId) &&
            Objects.equals(fom, that.fom) &&
            Objects.equals(tom, that.tom) &&
            Objects.equals(arbeidsforholdId, that.arbeidsforholdId) &&
            Objects.equals(eksternArbeidsforholdId, that.eksternArbeidsforholdId) &&
            Objects.equals(arbeidsforholdType, that.arbeidsforholdType) &&
            Objects.equals(aktørId, that.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiverId, fom, tom, arbeidsforholdId, eksternArbeidsforholdId, arbeidsforholdType, aktørId);
    }
}
