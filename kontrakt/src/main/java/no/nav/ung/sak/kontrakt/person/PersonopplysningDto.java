package no.nav.ung.sak.kontrakt.person;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PersonopplysningDto extends PersonIdentDto {

    /** Angitt annen part (dersom ytelse er knyttet til søker&lt;-&gt;annen part */
    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "annenPart")
    @Valid
    private PersonopplysningDto annenPart;

    /** Registrerte barn og fosterbarn. */
    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "barn")
    @Valid
    @Size(max = 30)
    private List<PersonopplysningDto> barn = new ArrayList<>();

    /** Barn søkt for. */
    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "barnSoktFor")
    @Valid
    @Size(max = 10)
    private List<PersonopplysningDto> barnSoktFor = new ArrayList<>();

    @JsonAlias("dødsdato")
    @JsonProperty(value = "dodsdato")
    @Valid
    private LocalDate dodsdato;

    @JsonProperty(value = "ektefelle")
    @Valid
    private PersonopplysningDto ektefelle;

    @JsonAlias("fødselsdato")
    @JsonProperty(value = "fodselsdato")
    @Valid
    private LocalDate fodselsdato;

    @JsonProperty(value = "harVerge")
    @Valid
    private boolean harVerge;

    @JsonAlias("kjønn")
    @JsonProperty(value = "navBrukerKjonn")
    @Valid
    private NavBrukerKjønn navBrukerKjonn;

    @JsonProperty(value = "navn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonProperty(value = "nummer")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer nummer;

    public PersonopplysningDto() {
        //
    }

    public PersonopplysningDto getAnnenPart() {
        return annenPart;
    }

    public List<PersonopplysningDto> getBarn() {
        return barn;
    }

    public List<PersonopplysningDto> getBarnSoktFor() {
        return barnSoktFor;
    }

    public LocalDate getDodsdato() {
        return dodsdato;
    }

    public PersonopplysningDto getEktefelle() {
        return ektefelle;
    }

    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public NavBrukerKjønn getNavBrukerKjonn() {
        return navBrukerKjonn;
    }

    public String getNavn() {
        return navn;
    }

    public Integer getNummer() {
        return nummer;
    }

    public boolean isHarVerge() {
        return harVerge;
    }

    public void setAnnenPart(PersonopplysningDto annenPart) {
        this.annenPart = annenPart;
    }

    public void setBarn(List<PersonopplysningDto> barn) {
        this.barn = barn;
    }

    public void setBarnSoktFor(List<PersonopplysningDto> barnSoktFor) {
        this.barnSoktFor = barnSoktFor;
    }

    public void setDodsdato(LocalDate dodsdato) {
        this.dodsdato = dodsdato;
    }

    public void setEktefelle(PersonopplysningDto ektefelle) {
        this.ektefelle = ektefelle;
    }

    public void setFodselsdato(LocalDate fodselsdato) {
        this.fodselsdato = fodselsdato;
    }

    public void setHarVerge(boolean harVerge) {
        this.harVerge = harVerge;
    }

    public void setNavBrukerKjonn(NavBrukerKjønn navBrukerKjonn) {
        this.navBrukerKjonn = navBrukerKjonn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setNummer(Integer nummer) {
        this.nummer = nummer;
    }
}
