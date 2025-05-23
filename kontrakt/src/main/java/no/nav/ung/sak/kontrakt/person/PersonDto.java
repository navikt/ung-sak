package no.nav.ung.sak.kontrakt.person;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PersonDto {

    @JsonProperty("alder")
    private Integer alder;

    @JsonProperty(value = "diskresjonskode", required = false)
    private String diskresjonskode;

    @JsonProperty("dodsdato")
    private LocalDate dodsdato;

    @JsonProperty("navn")
    private String navn;

    @JsonProperty("personnummer")
    private String personnummer;

    @JsonProperty("aktørId")
    private AktørId aktørId;

    public PersonDto() {
        //
    }

    public PersonDto(String navn,
                     Integer alder,
                     String personnummer,
                     String diskresjonskode,
                     LocalDate dodsdato,
                     AktørId aktørId) {
        this.navn = navn;
        this.alder = alder;
        this.personnummer = personnummer;
        this.diskresjonskode = diskresjonskode;
        this.dodsdato = dodsdato;
        this.aktørId = aktørId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PersonDto))
            return false;

        PersonDto personDto = (PersonDto) o;

        if (!navn.equals(personDto.navn))
            return false;
        if (!alder.equals(personDto.alder))
            return false;
        if (!personnummer.equals(personDto.personnummer))
            return false;
        if (!aktørId.equals(personDto.aktørId))
            return false;;
        return true;
    }

    public Integer getAlder() {
        return alder;
    }

    public String getDiskresjonskode() {
        return diskresjonskode;
    }

    public LocalDate getDodsdato() {
        return dodsdato;
    }

    public String getNavn() {
        return navn;
    }

    public String getPersonnummer() {
        return personnummer;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public int hashCode() {
        int result = navn.hashCode();
        result = 31 * result + alder.hashCode();
        result = 31 * result + personnummer.hashCode();
        return result;
    }

    public void setAlder(Integer alder) {
        this.alder = alder;
    }

    public void setDiskresjonskode(String diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
    }

    public void setDodsdato(LocalDate dodsdato) {
        this.dodsdato = dodsdato;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setPersonnummer(String personnummer) {
        this.personnummer = personnummer;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    @Override
    public String toString() {
        return "<navn='" + navn + '\'' +
            ", alder=" + alder +
            ", personnummer='" + personnummer + '\'' +
            '>';
    }
}
