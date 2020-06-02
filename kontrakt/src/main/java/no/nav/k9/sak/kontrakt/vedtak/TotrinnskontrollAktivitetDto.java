package no.nav.k9.sak.kontrakt.vedtak;

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
import no.nav.k9.sak.typer.OrgNummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TotrinnskontrollAktivitetDto {

    /**
     * @deprecated FIXME K9: Ikke send navn på kodeverdi i kontrakt. Hent fra kodeverk i stedet. Bruk {@link #opptjeningAktivitet} her i
     *             stedet også.
     */
    @Deprecated
    @JsonProperty(value = "aktivitetType")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String aktivitetType;

    @JsonProperty(value = "arbeidsgiverNavn")
    @Size(max = 1000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiverNavn;

    @JsonProperty(value = "erEndring")
    private Boolean erEndring;

    @JsonProperty(value = "godkjent")
    private boolean godkjent;

    @JsonProperty(value = "opptjeningAktivitet", required = true)
    @NotNull
    @Valid
    private OpptjeningAktivitetType opptjeningAktivitet;

    /** Orgnr dersom arbeidsgiver er virksomhet. */
    @JsonProperty(value = "orgnr")
    @Valid
    private OrgNummer orgnr;

    /** Fødselsdato dersom arbeidsgiver er privatperson. */
    @JsonProperty(value = "privatpersonFødselsdato")
    private LocalDate privatpersonFødselsdato;

    public TotrinnskontrollAktivitetDto() {
        // Tom
    }

    public String getAktivitetType() {
        return aktivitetType;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public Boolean getErEndring() {
        return erEndring;
    }

    public OrgNummer getOrgnr() {
        return orgnr;
    }

    public LocalDate getPrivatpersonFødselsdato() {
        return privatpersonFødselsdato;
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public void setAktivitetType(OpptjeningAktivitetType opptjeningAktivitetType) {
        this.opptjeningAktivitet = opptjeningAktivitetType;
        this.aktivitetType = opptjeningAktivitetType.getNavn();
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public void setErEndring(Boolean erEndring) {
        this.erEndring = erEndring;
    }

    public void setGodkjent(boolean godkjent) {
        this.godkjent = godkjent;
    }

    public void setOrgnr(OrgNummer orgnr) {
        this.orgnr = orgnr;
    }

    public void setPrivatpersonFødselsdato(LocalDate privatpersonFødselsdato) {
        this.privatpersonFødselsdato = privatpersonFødselsdato;
    }
}
