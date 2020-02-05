package no.nav.k9.sak.kontrakt.opptjening;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.constraints.Pattern;

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
    private OpptjeningAktivitetType aktivitetType;

    @JsonProperty(value = "originalFom")
    private LocalDate originalFom;

    @JsonProperty(value = "originalTom")
    private LocalDate originalTom;

    @JsonProperty(value = "opptjeningFom")
    private LocalDate opptjeningFom;

    @JsonProperty(value = "opptjeningTom")
    private LocalDate opptjeningTom;

    @JsonProperty(value = "arbeidsgiver")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiver;

    @JsonProperty(value = "arbeidsgiverNavn")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverNavn;

    @JsonProperty(value = "oppdragsgiverOrg")
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String oppdragsgiverOrg;

    @JsonProperty(value = "arbeidsgiverIdentifikator")
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value = "privatpersonNavn")
    @Pattern(regexp = "^[\\p{Alnum}.\\-\\p{Space}\\p{Sc}\\p{M}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String privatpersonNavn;

    @JsonProperty(value = "privatpersonFødselsdato")
    private LocalDate privatpersonFødselsdato;

    @JsonProperty(value = "arbeidsforholdRef")
    @Pattern(regexp = "^[\\p{Alnum}\\-_:.\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdRef;

    @JsonProperty(value = "stillingsandel")
    private BigDecimal stillingsandel;

    @JsonProperty(value = "naringRegistreringsdato")
    private LocalDate naringRegistreringsdato;

    @JsonProperty(value = "erManueltOpprettet")
    private Boolean erManueltOpprettet;

    @JsonProperty(value = "erGodkjent")
    private Boolean erGodkjent;

    @JsonProperty(value = "erEndret")
    private Boolean erEndret;

    @JsonProperty(value = "begrunnelse")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value = "erPeriodeEndret")
    private Boolean erPeriodeEndret;

    public OpptjeningAktivitetDto() {// NOSONAR
        //
    }

    public OpptjeningAktivitetDto(OpptjeningAktivitetType aktivitetType, LocalDate opptjeningFom,
                                  LocalDate opptjeningTom) {
        this.aktivitetType = aktivitetType;
        this.opptjeningFom = opptjeningFom;
        this.opptjeningTom = opptjeningTom;
    }

    public LocalDate getOriginalFom() {
        return originalFom;
    }

    public void setOriginalFom(LocalDate originalFom) {
        this.originalFom = originalFom;
    }

    public LocalDate getOriginalTom() {
        return originalTom;
    }

    public void setOriginalTom(LocalDate originalTom) {
        this.originalTom = originalTom;
    }

    public OpptjeningAktivitetType getAktivitetType() {
        return aktivitetType;
    }

    public void setAktivitetType(OpptjeningAktivitetType aktivitetType) {
        this.aktivitetType = aktivitetType;
    }

    public LocalDate getOpptjeningFom() {
        return opptjeningFom;
    }

    public void setOpptjeningFom(LocalDate opptjeningFom) {
        this.opptjeningFom = opptjeningFom;
    }

    public LocalDate getOpptjeningTom() {
        return opptjeningTom;
    }

    public void setOpptjeningTom(LocalDate opptjeningTom) {
        this.opptjeningTom = opptjeningTom;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public String getOppdragsgiverOrg() {
        return oppdragsgiverOrg;
    }

    public void setOppdragsgiverOrg(String oppdragsgiverOrg) {
        this.oppdragsgiverOrg = oppdragsgiverOrg;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public LocalDate getNaringRegistreringsdato() {
        return naringRegistreringsdato;
    }

    public void setNaringRegistreringsdato(LocalDate naringRegistreringsdato) {
        this.naringRegistreringsdato = naringRegistreringsdato;
    }

    public BigDecimal getStillingsandel() {
        return stillingsandel;
    }

    public void setStillingsandel(BigDecimal stillingsandel) {
        this.stillingsandel = stillingsandel;
    }

    public Boolean getErGodkjent() {
        return erGodkjent;
    }

    public void setErGodkjent(Boolean erGodkjent) {
        this.erGodkjent = erGodkjent;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Boolean getErManueltOpprettet() {
        return erManueltOpprettet;
    }

    public void setErManueltOpprettet(Boolean erManueltOpprettet) {
        this.erManueltOpprettet = erManueltOpprettet;
    }

    public Boolean getErEndret() {
        return erEndret;
    }

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public void setArbeidsforholdRef(String arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public void setErPeriodeEndret(Boolean erPeriodeEndret) {
        this.erPeriodeEndret = erPeriodeEndret;
    }

    public Boolean getErPeriodeEndret() {
        return erPeriodeEndret;
    }

    public String getPrivatpersonNavn() {
        return privatpersonNavn;
    }

    public void setPrivatpersonNavn(String privatpersonNavn) {
        this.privatpersonNavn = privatpersonNavn;
    }

    public LocalDate getPrivatpersonFødselsdato() {
        return privatpersonFødselsdato;
    }

    public void setPrivatpersonFødselsdato(LocalDate privatpersonFødselsdato) {
        this.privatpersonFødselsdato = privatpersonFødselsdato;
    }
}
