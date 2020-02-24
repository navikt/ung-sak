package no.nav.k9.sak.kontrakt.person;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.geografisk.AdresseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PersonadresseDto {

    @JsonProperty(value = "adresselinje1")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @Size(max = 100)
    private String adresselinje1;

    @JsonProperty(value = "adresselinje2")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @Size(max = 100)
    private String adresselinje2;

    @JsonProperty(value = "adresselinje3")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @Size(max = 100)
    private String adresselinje3;

    @JsonProperty(value = "adresseType", required = true)
    @NotNull
    @Valid
    private AdresseType adresseType;

    @JsonProperty(value = "land", required = true)
    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String land;

    @JsonProperty(value = "mottakerNavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String mottakerNavn;

    @JsonProperty(value = "postNummer")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @Size(max = 100)
    private String postNummer;

    @JsonProperty(value = "poststed")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @Size(max = 100)
    private String poststed;

    public PersonadresseDto() {
    }

    public String getAdresselinje1() {
        return adresselinje1;
    }

    public String getAdresselinje2() {
        return adresselinje2;
    }

    public String getAdresselinje3() {
        return adresselinje3;
    }

    public AdresseType getAdresseType() {
        return adresseType;
    }

    public String getLand() {
        return land;
    }

    public String getMottakerNavn() {
        return mottakerNavn;
    }

    public String getPostNummer() {
        return postNummer;
    }

    public String getPoststed() {
        return poststed;
    }

    public void setAdresselinje1(String adresselinje1) {
        this.adresselinje1 = adresselinje1;
    }

    public void setAdresselinje2(String adresselinje2) {
        this.adresselinje2 = adresselinje2;
    }

    public void setAdresselinje3(String adresselinje3) {
        this.adresselinje3 = adresselinje3;
    }

    public void setAdresseType(AdresseType adresseType) {
        this.adresseType = adresseType;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public void setMottakerNavn(String mottakerNavn) {
        this.mottakerNavn = mottakerNavn;
    }

    public void setPostNummer(String postNummer) {
        this.postNummer = postNummer;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }
}
