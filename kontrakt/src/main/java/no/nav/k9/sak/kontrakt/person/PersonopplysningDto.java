package no.nav.k9.sak.kontrakt.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;

public class PersonopplysningDto extends PersonIdentDto {

    private Integer nummer;
    private NavBrukerKjønn navBrukerKjonn;
    private LandkoderDto statsborgerskap;
    private AvklartPersonstatus avklartPersonstatus;
    private PersonstatusType personstatus;
    private SivilstandType sivilstand;
    private String navn;
    private LocalDate dodsdato;
    private LocalDate fodselsdato;
    private List<PersonadresseDto> adresser = new ArrayList<>();

    private Region region;
    private PersonopplysningDto annenPart;
    private PersonopplysningDto ektefelle;
    private List<PersonopplysningDto> barn = new ArrayList<>();
    private List<PersonopplysningDto> barnSoktFor = new ArrayList<>();
    private List<PersonopplysningDto> barnFraTpsRelatertTilSoknad = new ArrayList<>();
    private boolean harVerge;

    public NavBrukerKjønn getNavBrukerKjonn() {
        return navBrukerKjonn;
    }

    public void setNavBrukerKjonn(NavBrukerKjønn navBrukerKjonn) {
        this.navBrukerKjonn = navBrukerKjonn;
    }

    public LandkoderDto getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(LandkoderDto statsborgerskap) {
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
