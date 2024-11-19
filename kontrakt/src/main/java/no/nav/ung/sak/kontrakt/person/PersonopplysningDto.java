package no.nav.ung.sak.kontrakt.person;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.geografisk.Region;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.kodeverk.person.PersonstatusType;
import no.nav.ung.kodeverk.person.SivilstandType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PersonopplysningDto extends PersonIdentDto {

    @JsonProperty(value = "adresser")
    @Valid
    @Size(max = 100)
    private List<PersonadresseDto> adresser = new ArrayList<>();

    /** Angitt annen part (dersom ytelse er knyttet til søker&lt;-&gt;annen part */
    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "annenPart")
    @Valid
    private PersonopplysningDto annenPart;

    /** Angitt pleietrengende part (dersom ytelse er knyttet til søker&lt;-&gt;pleietrengende part */
    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "pleietrengendePart")
    @Valid
    private PersonopplysningDto pleietrengendePart;

    @JsonProperty(value = "avklartPersonstatus")
    @Valid
    private AvklartPersonstatus avklartPersonstatus;

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

    @JsonProperty(value = "personstatus")
    @Valid
    private PersonstatusType personstatus;

    @JsonProperty(value = "region")
    @Valid
    private Region region;

    @JsonProperty(value = "sivilstand")
    @Valid
    private SivilstandType sivilstand;

    @JsonProperty(value = "statsborgerskap")
    @Valid
    private Landkoder statsborgerskap;

    public PersonopplysningDto() {
        //
    }

    public List<PersonadresseDto> getAdresser() {
        return adresser;
    }

    public PersonopplysningDto getAnnenPart() {
        return annenPart;
    }

    public PersonopplysningDto getPleietrengendePart() {
        return pleietrengendePart;
    }

    public AvklartPersonstatus getAvklartPersonstatus() {
        return avklartPersonstatus;
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

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    public Region getRegion() {
        return region;
    }

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    public Landkoder getStatsborgerskap() {
        return statsborgerskap;
    }

    public boolean isHarVerge() {
        return harVerge;
    }

    public void setAdresser(List<PersonadresseDto> adresser) {
        this.adresser = adresser;
    }

    public void setAnnenPart(PersonopplysningDto annenPart) {
        this.annenPart = annenPart;
    }

    public void setAvklartPersonstatus(AvklartPersonstatus avklartPersonstatus) {
        this.avklartPersonstatus = avklartPersonstatus;
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

    public void setPersonstatus(PersonstatusType personstatus) {
        this.personstatus = personstatus;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setSivilstand(SivilstandType sivilstand) {
        this.sivilstand = sivilstand;
    }

    public void setStatsborgerskap(Landkoder statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    public void setPleietrengendePart(PersonopplysningDto dto) {
        this.pleietrengendePart = dto;
    }
}
