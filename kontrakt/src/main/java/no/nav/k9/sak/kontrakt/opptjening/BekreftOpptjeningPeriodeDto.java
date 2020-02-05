package no.nav.k9.sak.kontrakt.opptjening;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    
    @JsonProperty(value="aktivitetType")
    private OpptjeningAktivitetType aktivitetType;
    
    @JsonProperty(value="originalFom")
    private LocalDate originalFom;
    
    @JsonProperty(value="originalTom")
    private LocalDate originalTom;
    
    @JsonProperty(value="opptjeningFom")
    private LocalDate opptjeningFom;
    
    @JsonProperty(value="opptjeningTom")
    private LocalDate opptjeningTom;
    
    @JsonProperty(value="arbeidsgiverNavn")
    private String arbeidsgiverNavn;
    
    @JsonProperty(value="arbeidsgiverIdentifikator")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value="arbeidsforholdRef")
    private String arbeidsforholdRef;
    
    @JsonProperty(value="stillingsandel")
    private BigDecimal stillingsandel;
    
    @JsonProperty(value="naringRegistreringsdato")
    private LocalDate naringRegistreringsdato;
    
    @JsonProperty(value="erManueltOpprettet")
    private boolean erManueltOpprettet = false;
    
    @JsonProperty(value="erEndret")
    private boolean erEndret = false;
    
    @JsonProperty(value="erGodkjent")
    private Boolean erGodkjent;
    
    @JsonProperty(value="begrunnelse")
    private String begrunnelse;

    public BekreftOpptjeningPeriodeDto() {
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

    public Boolean getErEndret() {
        return erEndret;
    }

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret != null ? erEndret : false; // NOSONAR
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

    public BigDecimal getStillingsandel() {
        return stillingsandel;
    }

    public void setStillingsandel(BigDecimal stillingsandel) {
        this.stillingsandel = stillingsandel;
    }

    public LocalDate getNaringRegistreringsdato() {
        return naringRegistreringsdato;
    }

    public void setNaringRegistreringsdato(LocalDate naringRegistreringsdato) {
        this.naringRegistreringsdato = naringRegistreringsdato;
    }

    public boolean getErManueltOpprettet() {
        return erManueltOpprettet;
    }

    public void setErManueltOpprettet(Boolean erManueltOpprettet) {
        this.erManueltOpprettet = erManueltOpprettet != null ? erManueltOpprettet : false; // NOSONAR
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

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public void setArbeidsforholdRef(String arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }
}
