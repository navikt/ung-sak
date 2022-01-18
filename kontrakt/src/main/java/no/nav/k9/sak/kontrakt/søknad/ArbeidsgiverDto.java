package no.nav.k9.sak.kontrakt.søknad;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.OrgNummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class ArbeidsgiverDto {

    /** Angis når arbeidsgiver er privatperson, sammen med navn. */
    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;

    /** Angis når arbeidsgiver er privatperson, sammen med navn. */
    @JsonProperty(value = "fødselsdato")
    private LocalDate fødselsdato;

    /** Navn på arbeidgiver - virksomhet eller privatperson som arbeidsgiver. */
    @JsonProperty(value = "navn")
    @Size(max = 200)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonAlias({ "orgNummer" })
    @JsonProperty(value = "organisasjonsNummer")
    @Valid
    private OrgNummer organisasjonsnummer;

    public AktørId getAktørId() {
        return aktørId;
    }

    public String getNavn() {
        return navn;
    }

    public OrgNummer getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setOrganisasjonsnummer(OrgNummer organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }
}
