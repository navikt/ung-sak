package no.nav.k9.sak.kontrakt.opptjening;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
public class AvklarOpptjeningAktivitetDto {

    @JsonProperty(value = "aktivitetType")
    @NotNull
    private OpptjeningAktivitetType aktivitetType;

    @JsonProperty(value = "arbeidsforholdRef")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdRef;

    @JsonProperty(value = "arbeidsgiverIdentifikator")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value = "erEndret")
    private Boolean erEndret;

    @JsonProperty(value = "erGodkjent")
    private Boolean erGodkjent;

    @JsonProperty(value = "erManueltOpprettet")
    private Boolean erManueltOpprettet;

    @JsonProperty(value = "naringRegistreringsdato")
    private LocalDate naringRegistreringsdato;

    @JsonProperty(value = "oppdragsgiverOrg")
    @Size(min = 11, max = 13)
    @Pattern(regexp = "\\d{9}|\\d{13}")
    private String oppdragsgiverOrg;

    @JsonProperty(value = "opptjeningFom")
    @NotNull
    private LocalDate opptjeningFom;

    @JsonProperty(value = "opptjeningTom")
    @NotNull
    private LocalDate opptjeningTom;

    @JsonProperty(value = "originalFom")
    private LocalDate originalFom;

    @JsonProperty(value = "originalTom")
    private LocalDate originalTom;

    @JsonProperty(value = "stillingsandel")
    @Min(0)
    @Max(200)
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsandel;

    public AvklarOpptjeningAktivitetDto() {// NOSONAR
        // trengs for deserialisering av JSON
    }

    public OpptjeningAktivitetType getAktivitetType() {
        return aktivitetType;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getErEndret() {
        return erEndret;
    }

    public Boolean getErGodkjent() {
        return erGodkjent;
    }

    public Boolean getErManueltOpprettet() {
        return erManueltOpprettet;
    }

    public LocalDate getNaringRegistreringsdato() {
        return naringRegistreringsdato;
    }

    public String getOppdragsgiverOrg() {
        return oppdragsgiverOrg;
    }

    public LocalDate getOpptjeningFom() {
        return opptjeningFom;
    }

    public LocalDate getOpptjeningTom() {
        return opptjeningTom;
    }

    public LocalDate getOriginalFom() {
        return originalFom;
    }

    public LocalDate getOriginalTom() {
        return originalTom;
    }

    public BigDecimal getStillingsandel() {
        return stillingsandel;
    }

    public void setAktivitetType(OpptjeningAktivitetType aktivitetType) {
        this.aktivitetType = aktivitetType;
    }

    public void setArbeidsforholdRef(String arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret;
    }

    public void setErGodkjent(Boolean erGodkjent) {
        this.erGodkjent = erGodkjent;
    }

    public void setErManueltOpprettet(Boolean erManueltOpprettet) {
        this.erManueltOpprettet = erManueltOpprettet;
    }

    public void setNaringRegistreringsdato(LocalDate naringRegistreringsdato) {
        this.naringRegistreringsdato = naringRegistreringsdato;
    }

    public void setOppdragsgiverOrg(String oppdragsgiverOrg) {
        this.oppdragsgiverOrg = oppdragsgiverOrg;
    }

    public void setOpptjeningFom(LocalDate opptjeningFom) {
        this.opptjeningFom = opptjeningFom;
    }

    public void setOpptjeningTom(LocalDate opptjeningTom) {
        this.opptjeningTom = opptjeningTom;
    }

    public void setOriginalFom(LocalDate originalFom) {
        this.originalFom = originalFom;
    }

    public void setOriginalTom(LocalDate originalTom) {
        this.originalTom = originalTom;
    }

    public void setStillingsandel(BigDecimal stillingsandel) {
        this.stillingsandel = stillingsandel;
    }
}
