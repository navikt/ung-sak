package no.nav.k9.sak.kontrakt.opptjening;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BekreftOpptjeningPeriodeDto {

    @JsonProperty(value = "aktivitetType")
    @Valid
    private OpptjeningAktivitetType aktivitetType;

    @JsonProperty(value = "arbeidsforholdRef")
    private String arbeidsforholdRef;

    @JsonProperty(value = "arbeidsgiverIdentifikator")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value = "arbeidsgiverNavn")
    private String arbeidsgiverNavn;

    @JsonProperty(value = "begrunnelse")
    private String begrunnelse;

    @JsonProperty(value = "erEndret")
    private boolean erEndret = false;

    @JsonProperty(value = "erGodkjent")
    private Boolean erGodkjent;

    @JsonProperty(value = "erManueltOpprettet")
    private boolean erManueltOpprettet = false;

    @JsonProperty(value = "naringRegistreringsdato")
    private LocalDate naringRegistreringsdato;

    @JsonProperty(value = "opptjeningFom")
    private LocalDate opptjeningFom;

    @JsonProperty(value = "opptjeningTom")
    private LocalDate opptjeningTom;

    @JsonProperty(value = "originalFom")
    private LocalDate originalFom;

    @JsonProperty(value = "originalTom")
    private LocalDate originalTom;

    @JsonProperty(value = "stillingsandel")
    private BigDecimal stillingsandel;

    public BekreftOpptjeningPeriodeDto() {
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

    public boolean getErManueltOpprettet() {
        return erManueltOpprettet;
    }

    public LocalDate getNaringRegistreringsdato() {
        return naringRegistreringsdato;
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

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setErEndret(boolean erEndret) {
        this.erEndret = erEndret;
    }

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret != null ? erEndret : false; // NOSONAR
    }

    public void setErGodkjent(Boolean erGodkjent) {
        this.erGodkjent = erGodkjent;
    }

    public void setErManueltOpprettet(boolean erManueltOpprettet) {
        this.erManueltOpprettet = erManueltOpprettet;
    }

    public void setErManueltOpprettet(Boolean erManueltOpprettet) {
        this.erManueltOpprettet = erManueltOpprettet != null ? erManueltOpprettet : false; // NOSONAR
    }

    public void setNaringRegistreringsdato(LocalDate naringRegistreringsdato) {
        this.naringRegistreringsdato = naringRegistreringsdato;
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
