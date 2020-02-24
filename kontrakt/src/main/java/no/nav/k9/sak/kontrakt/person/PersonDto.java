package no.nav.k9.sak.kontrakt.person;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.person.PersonstatusType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PersonDto {

    @JsonProperty("alder")
    private Integer alder;

    @JsonProperty("diskresjonskode")
    private String diskresjonskode;

    @JsonProperty("dodsdato")
    private LocalDate dodsdato;

    @JsonProperty("erKvinne")
    private Boolean erKvinne;

    @JsonProperty("navn")
    private String navn;

    @JsonProperty("personnummer")
    private String personnummer;

    @JsonProperty("personstatusType")
    private PersonstatusType personstatusType;

    public PersonDto() {
        //
    }

    public PersonDto(String navn, Integer alder, String personnummer, boolean erKvinne, PersonstatusType personstatusType, String diskresjonskode,
                     LocalDate dodsdato) {
        this.navn = navn;
        this.alder = alder;
        this.personnummer = personnummer;
        this.erKvinne = erKvinne;
        this.personstatusType = personstatusType;
        this.diskresjonskode = diskresjonskode;
        this.dodsdato = dodsdato;
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
        return erKvinne.equals(personDto.erKvinne);
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

    @JsonGetter
    public Boolean getErDod() {
        return PersonstatusType.erDød(personstatusType);
    }

    public Boolean getErKvinne() {
        return erKvinne;
    }

    public String getNavn() {
        return navn;
    }

    public String getPersonnummer() {
        return personnummer;
    }

    public PersonstatusType getPersonstatusType() {
        return personstatusType;
    }

    @Override
    public int hashCode() {
        int result = navn.hashCode();
        result = 31 * result + alder.hashCode();
        result = 31 * result + personnummer.hashCode();
        result = 31 * result + erKvinne.hashCode();
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

    public void setErKvinne(Boolean erKvinne) {
        this.erKvinne = erKvinne;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setPersonnummer(String personnummer) {
        this.personnummer = personnummer;
    }

    public void setPersonstatusType(PersonstatusType personstatusType) {
        this.personstatusType = personstatusType;
    }

    @Override
    public String toString() {
        return "<navn='" + navn + '\'' +
            ", alder=" + alder +
            ", personnummer='" + personnummer + '\'' +
            ", erKvinne=" + erKvinne +
            '>';
    }
}
