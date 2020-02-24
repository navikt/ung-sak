package no.nav.k9.sak.kontrakt.opptjening;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
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
public class OpptjeningAktivitetDto {

    @JsonProperty(value = "aktivitetType")
    @Valid
    private OpptjeningAktivitetType aktivitetType;

    @JsonProperty(value = "arbeidsforholdRef")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\-_:.\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdRef;

    @JsonProperty(value = "arbeidsgiver")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiver;

    @JsonProperty(value = "arbeidsgiverIdentifikator")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value = "arbeidsgiverNavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverNavn;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value = "erEndret")
    private Boolean erEndret;

    @JsonProperty(value = "erGodkjent")
    private Boolean erGodkjent;

    @JsonProperty(value = "erManueltOpprettet")
    private Boolean erManueltOpprettet;

    @JsonProperty(value = "erPeriodeEndret")
    private Boolean erPeriodeEndret;

    @JsonProperty(value = "naringRegistreringsdato")
    private LocalDate naringRegistreringsdato;

    @JsonProperty(value = "oppdragsgiverOrg")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String oppdragsgiverOrg;

    @JsonProperty(value = "opptjeningFom")
    private LocalDate opptjeningFom;

    @JsonProperty(value = "opptjeningTom")
    private LocalDate opptjeningTom;

    @JsonProperty(value = "originalFom")
    private LocalDate originalFom;

    @JsonProperty(value = "originalTom")
    private LocalDate originalTom;

    @JsonProperty(value = "privatpersonFødselsdato")
    private LocalDate privatpersonFødselsdato;

    @JsonProperty(value = "privatpersonNavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}.\\-\\p{Space}\\p{Sc}\\p{M}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String privatpersonNavn;

    @JsonProperty(value = "stillingsandel")
    @DecimalMin("0.00")
    @DecimalMax("500.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsandel;

    public OpptjeningAktivitetDto() {// NOSONAR
        //
    }

    public OpptjeningAktivitetDto(OpptjeningAktivitetType aktivitetType, LocalDate opptjeningFom,
                                  LocalDate opptjeningTom) {
        this.aktivitetType = aktivitetType;
        this.opptjeningFom = opptjeningFom;
        this.opptjeningTom = opptjeningTom;
    }

    public OpptjeningAktivitetType getAktivitetType() {
        return aktivitetType;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
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

    public Boolean getErPeriodeEndret() {
        return erPeriodeEndret;
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

    public LocalDate getPrivatpersonFødselsdato() {
        return privatpersonFødselsdato;
    }

    public String getPrivatpersonNavn() {
        return privatpersonNavn;
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

    public void setArbeidsgiver(String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
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

    public void setErPeriodeEndret(Boolean erPeriodeEndret) {
        this.erPeriodeEndret = erPeriodeEndret;
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

    public void setPrivatpersonFødselsdato(LocalDate privatpersonFødselsdato) {
        this.privatpersonFødselsdato = privatpersonFødselsdato;
    }

    public void setPrivatpersonNavn(String privatpersonNavn) {
        this.privatpersonNavn = privatpersonNavn;
    }

    public void setStillingsandel(BigDecimal stillingsandel) {
        this.stillingsandel = stillingsandel;
    }
}
