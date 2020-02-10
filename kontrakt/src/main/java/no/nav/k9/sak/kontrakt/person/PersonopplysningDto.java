package no.nav.k9.sak.kontrakt.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PersonopplysningDto extends PersonIdentDto {

    @JsonProperty(value = "nummer")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer nummer;

    @JsonAlias("kjønn")
    @JsonProperty(value = "navBrkerKjonn")
    @Valid
    private NavBrukerKjønn navBrukerKjonn;

    @JsonProperty(value = "statsborgerskap")
    @Valid
    private Landkoder statsborgerskap;

    @JsonProperty(value = "avklartPersonstatus")
    @Valid
    private AvklartPersonstatus avklartPersonstatus;

    @JsonProperty(value = "pesonstatus")
    @Valid
    private PersonstatusType personstatus;

    @JsonProperty(value = "sivilstand")
    @Valid
    private SivilstandType sivilstand;

    @JsonProperty(value = "navn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;

    @JsonAlias("dødsdato")
    @JsonProperty(value = "dodsdato")
    @Valid
    private LocalDate dodsdato;

    @JsonAlias("fødselsdato")
    @JsonProperty(value = "fodselsdato")
    @Valid
    private LocalDate fodselsdato;

    @JsonProperty(value = "adresser")
    @Valid
    @Size(max = 100)
    private List<PersonadresseDto> adresser = new ArrayList<>();

    @JsonProperty(value = "region")
    @Valid
    private Region region;

    @JsonProperty(value = "annenPart")
    @Valid
    private PersonopplysningDto annenPart;

    @JsonProperty(value = "ektefelle")
    @Valid
    private PersonopplysningDto ektefelle;

    @JsonProperty(value = "barn")
    @Valid
    @Size(max = 30)
    private List<PersonopplysningDto> barn = new ArrayList<>();

    @JsonProperty(value = "barnSoktFor")
    @Valid
    @Size(max = 10)
    private List<PersonopplysningDto> barnSoktFor = new ArrayList<>();

    @JsonProperty(value = "barnFraTpsRelatertTilSoknad")
    @Valid
    @Size(max = 10)
    private List<PersonopplysningDto> barnFraTpsRelatertTilSoknad = new ArrayList<>();

    @JsonProperty(value = "harVerge")
    @Valid
    private boolean harVerge;

    public NavBrukerKjønn getNavBrukerKjonn() {
        return navBrukerKjonn;
    }

    public void setNavBrukerKjonn(NavBrukerKjønn navBrukerKjonn) {
        this.navBrukerKjonn = navBrukerKjonn;
    }

    public Landkoder getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(Landkoder statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    public void setPersonstatus(PersonstatusType personstatus) {
        this.personstatus = personstatus;
    }

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(SivilstandType sivilstand) {
        this.sivilstand = sivilstand;
    }

    public Integer getNummer() {
        return nummer;
    }

    public void setNummer(Integer nummer) {
        this.nummer = nummer;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public LocalDate getDodsdato() {
        return dodsdato;
    }

    public void setDodsdato(LocalDate dodsdato) {
        this.dodsdato = dodsdato;
    }

    public List<PersonadresseDto> getAdresser() {
        return adresser;
    }

    public void setAdresser(List<PersonadresseDto> adresser) {
        this.adresser = adresser;
    }

    public PersonopplysningDto getAnnenPart() {
        return annenPart;
    }

    public void setAnnenPart(PersonopplysningDto annenPart) {
        this.annenPart = annenPart;
    }

    public PersonopplysningDto getEktefelle() {
        return ektefelle;
    }

    public void setEktefelle(PersonopplysningDto ektefelle) {
        this.ektefelle = ektefelle;
    }

    public List<PersonopplysningDto> getBarn() {
        return barn;
    }

    public void setBarn(List<PersonopplysningDto> barn) {
        this.barn = barn;
    }

    public List<PersonopplysningDto> getBarnSoktFor() {
        return barnSoktFor;
    }

    public void setBarnSoktFor(List<PersonopplysningDto> barnSoktFor) {
        this.barnSoktFor = barnSoktFor;
    }

    public List<PersonopplysningDto> getBarnFraTpsRelatertTilSoknad() {
        return barnFraTpsRelatertTilSoknad;
    }

    public void setBarnFraTpsRelatertTilSoknad(List<PersonopplysningDto> barnFraTpsRelatertTilSoknad) {
        this.barnFraTpsRelatertTilSoknad = barnFraTpsRelatertTilSoknad;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public void setFodselsdato(LocalDate fodselsdato) {
        this.fodselsdato = fodselsdato;
    }

    public boolean isHarVerge() {
        return harVerge;
    }

    public void setHarVerge(boolean harVerge) {
        this.harVerge = harVerge;
    }

    public AvklartPersonstatus getAvklartPersonstatus() {
        return avklartPersonstatus;
    }

    public void setAvklartPersonstatus(AvklartPersonstatus avklartPersonstatus) {
        this.avklartPersonstatus = avklartPersonstatus;
    }
}
