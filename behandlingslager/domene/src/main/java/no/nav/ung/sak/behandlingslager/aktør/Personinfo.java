package no.nav.ung.sak.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.geografisk.Region;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.kodeverk.person.PersonstatusType;
import no.nav.ung.kodeverk.person.SivilstandType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

public class Personinfo {

    private AktørId aktørId;
    private String navn;
    private PersonIdent personIdent;
    private String adresse;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private PersonstatusType personstatus;
    private NavBrukerKjønn kjønn;
    private Set<Familierelasjon> familierelasjoner = Collections.emptySet();
    private Statsborgerskap statsborgerskap;
    private Region region;
    private String utlandsadresse;
    private String geografiskTilknytning;
    private String diskresjonskode;
    private Språkkode foretrukketSpråk;
    private String adresseLandkode;
    private Landkoder landkode;

    private List<Adresseinfo> adresseInfoList = new ArrayList<>();
    private SivilstandType sivilstand;

    private List<DeltBosted> deltBostedList = new ArrayList<>();

    private Personinfo() {
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public String getNavn() {
        return navn;
    }

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public int getAlder(LocalDate dato) {
        return (int) ChronoUnit.YEARS.between(fødselsdato, dato);
    }

    public int getAlderIDag(){
        return getAlder(LocalDate.now());
    }

    public Set<Familierelasjon> getFamilierelasjoner() {
        return Collections.unmodifiableSet(familierelasjoner);
    }

    public boolean erKvinne() {
        return kjønn.equals(NavBrukerKjønn.KVINNE);
    }

    public String getAdresse() {
        return adresse;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public Statsborgerskap getStatsborgerskap() {
        return statsborgerskap;
    }

    public String getUtlandsadresse() {
        return utlandsadresse;
    }

    public Region getRegion() {
        return region;
    }

    public String getAdresseLandkode() {
        return adresseLandkode;
    }

    public Språkkode getForetrukketSpråk() {
        return foretrukketSpråk;
    }

    public String getGeografiskTilknytning() {
        return geografiskTilknytning;
    }

    public String getDiskresjonskode() {
        return diskresjonskode;
    }

    public List<Adresseinfo> getAdresseInfoList() {
        return adresseInfoList;
    }

    public SivilstandType getSivilstandType() {
        return sivilstand;
    }

    public Landkoder getLandkode() {
        return landkode;
    }

    public List<DeltBosted> getDeltBostedList() {
        return deltBostedList;
    }

    @Override
    public String toString() {
        // tar ikke med aktørId/fnr/personident i toString, så det ikke lekker i logger etc.
        return getClass().getSimpleName() + "<fødselsdato=" + fødselsdato +
            ", statsborgerskap=" + statsborgerskap +
            ", landkode=" + landkode + ">"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static class Builder {
        private Personinfo personinfoMal;

        public Builder() {
            personinfoMal = new Personinfo();
        }

        public Builder medAktørId(AktørId aktørId) {
            personinfoMal.aktørId = aktørId;
            return this;
        }

        public Builder medNavn(String navn) {
            personinfoMal.navn = navn;
            return this;
        }

        /**
         * @deprecated Bruk {@link #medPersonIdent(PersonIdent)} i stedet!
         */
        @Deprecated
        public Builder medFnr(String fnr) {
            personinfoMal.personIdent = PersonIdent.fra(fnr);
            return this;
        }

        public Builder medPersonIdent(PersonIdent fnr) {
            personinfoMal.personIdent = fnr;
            return this;
        }

        public Builder medAdresse(String adresse) {
            personinfoMal.adresse = adresse;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            personinfoMal.fødselsdato = fødselsdato;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            personinfoMal.dødsdato = dødsdato;
            return this;
        }

        public Builder medNavBrukerKjønn(NavBrukerKjønn kjønn) {
            personinfoMal.kjønn = kjønn;
            return this;
        }

        public Builder medPersonstatusType(PersonstatusType personstatus) {
            personinfoMal.personstatus = personstatus;
            return this;
        }

        public Builder medKjønn(NavBrukerKjønn kjønn) {
            personinfoMal.kjønn = kjønn;
            return this;
        }

        public Builder medFamilierelasjon(Set<Familierelasjon> familierelasjon) {
            personinfoMal.familierelasjoner = familierelasjon;
            return this;
        }

        public Builder medStatsborgerskap(Statsborgerskap statsborgerskap) {
            personinfoMal.statsborgerskap = statsborgerskap;
            return this;
        }

        public Builder medRegion(Region region) {
            personinfoMal.region = region;
            return this;
        }

        public Builder medUtlandsadresse(String utlandsadresse) {
            personinfoMal.utlandsadresse = utlandsadresse;
            return this;
        }

        public Builder medGegrafiskTilknytning(String geoTilkn) {
            personinfoMal.geografiskTilknytning = geoTilkn;
            return this;
        }

        public Builder medDiskresjonsKode(String diskresjonsKode) {
            personinfoMal.diskresjonskode = diskresjonsKode;
            return this;
        }

        public Builder medForetrukketSpråk(Språkkode språk) {
            personinfoMal.foretrukketSpråk = språk;
            return this;
        }

        public Builder medAdresseLandkode(String adresseLandkode) {
            personinfoMal.adresseLandkode = adresseLandkode;
            return this;
        }

        public Builder medAdresseInfoList(List<Adresseinfo> adresseinfoArrayList) {
            personinfoMal.adresseInfoList = adresseinfoArrayList;
            return this;
        }

        public Builder medSivilstandType(SivilstandType sivilstandType) {
            personinfoMal.sivilstand = sivilstandType;
            return this;
        }

        public Builder medLandkode(Landkoder landkode) {
            personinfoMal.landkode = landkode;
            return this;
        }

        public Builder medDeltBostedList(List<DeltBosted> deltBostedList) {
            personinfoMal.deltBostedList = deltBostedList;
            return this;
        }

        public Personinfo build() {
            requireNonNull(personinfoMal.aktørId, "Navbruker må ha aktørId"); //$NON-NLS-1$
            requireNonNull(personinfoMal.personIdent, "Navbruker må ha fødselsnummer"); //$NON-NLS-1$
            requireNonNull(personinfoMal.navn, "Navbruker må ha navn"); //$NON-NLS-1$
            requireNonNull(personinfoMal.fødselsdato, "Navbruker må ha fødselsdato"); //$NON-NLS-1$
            return personinfoMal;
        }

    }

}
