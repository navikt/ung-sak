package no.nav.k9.sak.kontrakt.søknad;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.OrgNummer;

/**
 * Inntektsmeldinger som søker har rapport at ikke vil komme fra angitt arbeidsgiver
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class InntektsmeldingSomIkkeKommerDto {

    /** AktørId dersom arbeisgiver er virksomhet. */
    // AktørId (13-tall) for person-arbeidsgiver
    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "brukerHarSagtAtIkkeKommer")
    private boolean brukerHarSagtAtIkkeKommer;

    /** Orgnummer dersom arbeisgiver er virksomhet. */
    @JsonAlias({ "orgNummer" })
    @JsonProperty(value = "organisasjonsnummer")
    @Valid
    private OrgNummer organisasjonsnummer;

    public InntektsmeldingSomIkkeKommerDto() { // NOSONAR
        // Jackson
    }

    public InntektsmeldingSomIkkeKommerDto(OrgNummer organisasjonsnummer, boolean brukerHarSagtAtIkkeKommer) {
        this.organisasjonsnummer = organisasjonsnummer;
        this.brukerHarSagtAtIkkeKommer = brukerHarSagtAtIkkeKommer;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public OrgNummer getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public boolean isBrukerHarSagtAtIkkeKommer() {
        return brukerHarSagtAtIkkeKommer;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public void setBrukerHarSagtAtIkkeKommer(boolean brukerHarSagtAtIkkeKommer) {
        this.brukerHarSagtAtIkkeKommer = brukerHarSagtAtIkkeKommer;
    }

    public void setOrganisasjonsnummer(OrgNummer organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }
}
