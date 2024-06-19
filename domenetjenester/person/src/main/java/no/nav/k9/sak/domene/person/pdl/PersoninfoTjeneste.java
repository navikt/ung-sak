package no.nav.k9.sak.domene.person.pdl;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Bostedsadresse;
import no.nav.k9.felles.integrasjon.pdl.BostedsadresseResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.DeltBostedResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Doedsfall;
import no.nav.k9.felles.integrasjon.pdl.DoedsfallResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Foedselsdato;
import no.nav.k9.felles.integrasjon.pdl.FoedselsdatoResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.FolkeregistermetadataResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Folkeregisterpersonstatus;
import no.nav.k9.felles.integrasjon.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonRolle;
import no.nav.k9.felles.integrasjon.pdl.HentPersonQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.InnflyttingTilNorgeResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Kjoenn;
import no.nav.k9.felles.integrasjon.pdl.KjoennResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.KjoennType;
import no.nav.k9.felles.integrasjon.pdl.Kontaktadresse;
import no.nav.k9.felles.integrasjon.pdl.KontaktadresseResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Matrikkeladresse;
import no.nav.k9.felles.integrasjon.pdl.MatrikkeladresseResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Navn;
import no.nav.k9.felles.integrasjon.pdl.NavnResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Oppholdsadresse;
import no.nav.k9.felles.integrasjon.pdl.OppholdsadresseResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.felles.integrasjon.pdl.Person;
import no.nav.k9.felles.integrasjon.pdl.PersonBostedsadresseParametrizedInput;
import no.nav.k9.felles.integrasjon.pdl.PersonFolkeregisterpersonstatusParametrizedInput;
import no.nav.k9.felles.integrasjon.pdl.PersonKontaktadresseParametrizedInput;
import no.nav.k9.felles.integrasjon.pdl.PersonOppholdsadresseParametrizedInput;
import no.nav.k9.felles.integrasjon.pdl.PersonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.PersonStatsborgerskapParametrizedInput;
import no.nav.k9.felles.integrasjon.pdl.PostadresseIFrittFormat;
import no.nav.k9.felles.integrasjon.pdl.PostadresseIFrittFormatResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Postboksadresse;
import no.nav.k9.felles.integrasjon.pdl.PostboksadresseResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Sivilstand;
import no.nav.k9.felles.integrasjon.pdl.SivilstandResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Sivilstandstype;
import no.nav.k9.felles.integrasjon.pdl.Statsborgerskap;
import no.nav.k9.felles.integrasjon.pdl.StatsborgerskapResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.UkjentBosted;
import no.nav.k9.felles.integrasjon.pdl.UkjentBostedResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.UtenlandskAdresse;
import no.nav.k9.felles.integrasjon.pdl.UtenlandskAdresseIFrittFormat;
import no.nav.k9.felles.integrasjon.pdl.UtenlandskAdresseIFrittFormatResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.UtenlandskAdresseResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.UtflyttingFraNorgeResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Vegadresse;
import no.nav.k9.felles.integrasjon.pdl.VegadresseResponseProjection;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.DeltBosted;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;

@ApplicationScoped
public class PersoninfoTjeneste {

    private static final String HARDKODET_POSTNR = "XXXX";

    private static final Set<Sivilstandstype> JURIDISK_GIFT = Set.of(Sivilstandstype.GIFT, Sivilstandstype.SEPARERT,
        Sivilstandstype.REGISTRERT_PARTNER, Sivilstandstype.SEPARERT_PARTNER);

    private static final Map<Sivilstandstype, SivilstandType> SIVSTAND_FRA_FREG = ofEntries(
        entry(Sivilstandstype.UOPPGITT, SivilstandType.UOPPGITT),
        entry(Sivilstandstype.UGIFT, SivilstandType.UGIFT),
        entry(Sivilstandstype.GIFT, SivilstandType.GIFT),
        entry(Sivilstandstype.ENKE_ELLER_ENKEMANN, SivilstandType.ENKEMANN),
        entry(Sivilstandstype.SKILT, SivilstandType.SKILT),
        entry(Sivilstandstype.SEPARERT, SivilstandType.SEPARERT),
        entry(Sivilstandstype.REGISTRERT_PARTNER, SivilstandType.REGISTRERT_PARTNER),
        entry(Sivilstandstype.SEPARERT_PARTNER, SivilstandType.SEPARERT_PARTNER),
        entry(Sivilstandstype.SKILT_PARTNER, SivilstandType.SKILT_PARTNER),
        entry(Sivilstandstype.GJENLEVENDE_PARTNER, SivilstandType.GJENLEVENDE_PARTNER));

    private static final Map<ForelderBarnRelasjonRolle, RelasjonsRolleType> ROLLE_FRA_FREG_ROLLE = ofEntries(
        entry(ForelderBarnRelasjonRolle.BARN, RelasjonsRolleType.BARN),
        entry(ForelderBarnRelasjonRolle.MOR, RelasjonsRolleType.MORA),
        entry(ForelderBarnRelasjonRolle.FAR, RelasjonsRolleType.FARA),
        entry(ForelderBarnRelasjonRolle.MEDMOR, RelasjonsRolleType.MEDMOR));

    private static final Map<Sivilstandstype, RelasjonsRolleType> ROLLE_FRA_FREG_STAND = ofEntries(
        entry(Sivilstandstype.GIFT, RelasjonsRolleType.EKTE),
        entry(Sivilstandstype.REGISTRERT_PARTNER, RelasjonsRolleType.REGISTRERT_PARTNER));

    private PdlKlient pdlKlient;

    PersoninfoTjeneste() {
        // CDI
    }

    @Inject
    public PersoninfoTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    private static Adresseinfo mapFriAdresseUtland(UtenlandskAdresseIFrittFormat utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var postlinje = hvisfinnes(utenlandskAdresse.getPostkode())
            + hvisfinnes(utenlandskAdresse.getByEllerStedsnavn());
        var sisteline = utenlandskAdresse.getAdresselinje3() != null ? postlinje + utenlandskAdresse.getLandkode()
            : (utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getLandkode() : null);
        return Adresseinfo.builder(AdresseType.POSTADRESSE_UTLAND)
            .medAdresselinje1(utenlandskAdresse.getAdresselinje1())
            .medAdresselinje2(utenlandskAdresse.getAdresselinje2() != null
                ? utenlandskAdresse.getAdresselinje2().toUpperCase()
                : postlinje)
            .medAdresselinje3(utenlandskAdresse.getAdresselinje3() != null
                ? utenlandskAdresse.getAdresselinje3().toUpperCase()
                : (utenlandskAdresse.getAdresselinje2() != null ? postlinje : utenlandskAdresse.getLandkode()))
            .medAdresselinje4(sisteline)
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private static String mapNavn(Navn navn) {
        return navn.getEtternavn() + " " + navn.getFornavn()
            + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }

    private static NavBrukerKjønn mapKjønn(Person person) {
        var kode = person.getKjoenn().stream()
            .map(Kjoenn::getKjoenn)
            .filter(Objects::nonNull)
            .findFirst().orElse(KjoennType.UKJENT);
        if (KjoennType.MANN.equals(kode))
            return NavBrukerKjønn.MANN;
        return KjoennType.KVINNE.equals(kode) ? NavBrukerKjønn.KVINNE : NavBrukerKjønn.UDEFINERT;
    }

    private static Landkoder mapStatsborgerskap(List<Statsborgerskap> statsborgerskap) {
        List<Landkoder> alleLand = statsborgerskap.stream()
            .map(Statsborgerskap::getLand)
            .map(kode -> Optional.ofNullable(Landkoder.fraKode(kode)).orElse(Landkoder.UDEFINERT))
            .toList();
        return alleLand.stream().anyMatch(Landkoder.NOR::equals) ? Landkoder.NOR
            : alleLand.stream().findFirst().orElse(Landkoder.UOPPGITT_UKJENT);
    }

    private static Set<Familierelasjon> mapFamilierelasjoner(
        List<no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjon> forelderBarnRelasjoner,
        List<Sivilstand> sivilstandliste) {
        Set<Familierelasjon> relasjoner = new HashSet<>();

        forelderBarnRelasjoner.stream()
            .filter(r -> r.getRelatertPersonsIdent() != null)
            .map(r -> new Familierelasjon(
                new PersonIdent(r.getRelatertPersonsIdent()),
                mapRelasjonsrolle(r.getRelatertPersonsRolle())))
            .forEach(relasjoner::add);
        sivilstandliste.stream()
            .filter(rel -> JURIDISK_GIFT.contains(rel.getType()))
            .filter(rel -> rel.getRelatertVedSivilstand() != null)
            .map(r -> new Familierelasjon(new PersonIdent(r.getRelatertVedSivilstand()),
                mapRelasjonsrolle(r.getType())))
            .forEach(relasjoner::add);
        return relasjoner;
    }

    private static RelasjonsRolleType mapRelasjonsrolle(ForelderBarnRelasjonRolle type) {
        return ROLLE_FRA_FREG_ROLLE.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private static RelasjonsRolleType mapRelasjonsrolle(Sivilstandstype type) {
        return ROLLE_FRA_FREG_STAND.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private static Adresseinfo mapUkjentadresse(UkjentBosted ukjentBosted) {
        return Adresseinfo.builder(AdresseType.UKJENT_ADRESSE).medLand(Landkoder.UOPPGITT_UKJENT.getKode()).build();
    }

    private static Adresseinfo mapUtenlandskadresse(AdresseType type, UtenlandskAdresse utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var linje1 = hvisfinnes(utenlandskAdresse.getAdressenavnNummer())
            + hvisfinnes(utenlandskAdresse.getBygningEtasjeLeilighet())
            + hvisfinnes(utenlandskAdresse.getPostboksNummerNavn());
        var linje2 = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getBySted())
            + hvisfinnes(utenlandskAdresse.getRegionDistriktOmraade());
        return Adresseinfo.builder(type)
            .medAdresselinje1(linje1)
            .medAdresselinje2(linje2)
            .medAdresselinje3(utenlandskAdresse.getLandkode())
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private static String hvisfinnes(Object object) {
        return object == null ? "" : " " + object.toString().trim().toUpperCase();
    }

    private static String hvisfinnes2(Object object1, Object object2) {
        if (object1 == null && object2 == null)
            return "";
        if (object1 != null && object2 != null)
            return " " + object1.toString().trim().toUpperCase() + object2.toString().trim().toUpperCase();
        return object2 == null ? " " + object1.toString().trim().toUpperCase()
            : " " + object2.toString().trim().toUpperCase();
    }

    static List<AdressePeriode> periodiserAdresse(List<AdressePeriode> perioder) {
        var adresseTypePerioder = perioder.stream()
            .collect(Collectors.groupingBy(ap -> forSortering(ap.getAdresse().getAdresseType()),
                Collectors.mapping(AdressePeriode::getGyldighetsperiode, Collectors.toList())));
        return perioder.stream()
            .map(p -> new AdressePeriode(
                finnFraPerioder(adresseTypePerioder.get(forSortering(p.getAdresse().getAdresseType())),
                    p.getGyldighetsperiode()),
                p.getAdresse()))
            .filter(a -> !a.getGyldighetsperiode().getFom().isAfter(a.getGyldighetsperiode().getTom()))
            .collect(Collectors.toList());
    }

    private static AdresseType forSortering(AdresseType type) {
        if (Set.of(AdresseType.BOSTEDSADRESSE, AdresseType.UKJENT_ADRESSE).contains(type))
            return AdresseType.BOSTEDSADRESSE;
        if (Set.of(AdresseType.POSTADRESSE, AdresseType.POSTADRESSE_UTLAND).contains(type))
            return AdresseType.POSTADRESSE;
        return AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE;
    }

    private static Gyldighetsperiode finnFraPerioder(List<Gyldighetsperiode> alleperioder, Gyldighetsperiode periode) {
        if (alleperioder.stream()
            .noneMatch(p -> p.getFom().isAfter(periode.getFom()) && p.getFom().isBefore(periode.getTom())))
            return periode;
        var tom = alleperioder.stream()
            .map(Gyldighetsperiode::getFom)
            .filter(d -> d.isAfter(periode.getFom()))
            .min(Comparator.naturalOrder())
            .map(d -> d.minusDays(1)).orElse(Tid.TIDENES_ENDE);
        return Gyldighetsperiode.innenfor(periode.getFom(), tom);
    }

    private static Gyldighetsperiode periodeFraDates(Date dateFom, Date dateTom) {
        var gyldigTil = dateTom == null ? null
            : LocalDateTime.ofInstant(dateTom.toInstant(), ZoneId.systemDefault()).toLocalDate();
        var gyldigFra = dateFom == null ? null
            : LocalDateTime.ofInstant(dateFom.toInstant(), ZoneId.systemDefault()).toLocalDate();
        return Gyldighetsperiode.innenfor(gyldigFra, gyldigTil);
    }

    private static AdressePeriode mapAdresseinfoTilAdressePeriode(Gyldighetsperiode periode, Adresseinfo adresseinfo) {
        return AdressePeriode.builder()
            .medGyldighetsperiode(periode)
            .medMatrikkelId(adresseinfo.getMatrikkelId())
            .medAdresselinje1(adresseinfo.getAdresselinje1())
            .medAdresselinje2(adresseinfo.getAdresselinje2())
            .medAdresselinje3(adresseinfo.getAdresselinje3())
            .medAdresselinje4(adresseinfo.getAdresselinje4())
            .medAdresseType(adresseinfo.getGjeldendePostadresseType())
            .medPostnummer(adresseinfo.getPostNr())
            .medLand(adresseinfo.getLand())
            .build();
    }

    private static StatsborgerskapPeriode mapStatsborgerskapHistorikk(Statsborgerskap statsborgerskap) {
        var gyldigTil = statsborgerskap.getGyldigTilOgMed() == null ? null
            : LocalDate.parse(statsborgerskap.getGyldigTilOgMed(), DateTimeFormatter.ISO_LOCAL_DATE);
        var gyldigFra = statsborgerskap.getGyldigFraOgMed() == null ? null
            : LocalDate.parse(statsborgerskap.getGyldigFraOgMed(), DateTimeFormatter.ISO_LOCAL_DATE);
        return new StatsborgerskapPeriode(Gyldighetsperiode.innenfor(gyldigFra, gyldigTil),
            new no.nav.k9.sak.behandlingslager.aktør.Statsborgerskap(statsborgerskap.getLand()));
    }

    private static PersonstatusPeriode mapPersonstatusHistorisk(Folkeregisterpersonstatus status) {
        var ajourFom = status.getFolkeregistermetadata().getAjourholdstidspunkt(); // TODO evaluer
        var gyldigFom = status.getFolkeregistermetadata().getGyldighetstidspunkt();
        Date brukFom;
        if (ajourFom != null && gyldigFom != null) {
            brukFom = ajourFom.before(gyldigFom) ? ajourFom : gyldigFom;
        } else {
            brukFom = gyldigFom != null ? gyldigFom : ajourFom;
        }
        var periode = periodeFraDates(brukFom, status.getFolkeregistermetadata().getOpphoerstidspunkt());
        return new PersonstatusPeriode(periode, PersonstatusType.fraFregPersonstatus(status.getStatus()));
    }

    private static List<PersonstatusPeriode> periodiserPersonstatus(List<PersonstatusPeriode> perioder) {
        var gyldighetsperioder = perioder.stream().map(PersonstatusPeriode::getGyldighetsperiode)
            .collect(Collectors.toList());
        return perioder.stream()
            .map(p -> new PersonstatusPeriode(finnFraPerioder(gyldighetsperioder, p.getGyldighetsperiode()),
                p.getPersonstatus()))
            .collect(Collectors.toList());
    }

    public Personinfo hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent) {

        var query = new HentPersonQueryRequest();
        query.setIdent(personIdent.getIdent());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato())
            .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status())
            .innflyttingTilNorge(new InnflyttingTilNorgeResponseProjection().fraflyttingsland())
            .utflyttingFraNorge(new UtflyttingFraNorgeResponseProjection().tilflyttingsland())
            .kjoenn(new KjoennResponseProjection().kjoenn())
            .sivilstand(new SivilstandResponseProjection().relatertVedSivilstand().type())
            .statsborgerskap(new StatsborgerskapResponseProjection().land())
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsRolle()
                .relatertPersonsIdent().minRolleForPerson())
            .bostedsadresse(new BostedsadresseResponseProjection().gyldigFraOgMed().angittFlyttedato()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer()
                    .husbokstav().postnummer())
                .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer()
                    .tilleggsnavn().postnummer())
                .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                .utenlandskAdresse(
                    new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet()
                        .postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
            .oppholdsadresse(new OppholdsadresseResponseProjection().gyldigFraOgMed()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer()
                    .husbokstav().postnummer())
                .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer()
                    .tilleggsnavn().postnummer())
                .utenlandskAdresse(
                    new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet()
                        .postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
            .kontaktadresse(new KontaktadresseResponseProjection().type().gyldigFraOgMed()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer()
                    .husbokstav().postnummer())
                .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                .postadresseIFrittFormat(new PostadresseIFrittFormatResponseProjection().adresselinje1()
                    .adresselinje2().adresselinje3().postnummer())
                .utenlandskAdresse(
                    new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet()
                        .postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode())
                .utenlandskAdresseIFrittFormat(
                    new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2()
                        .adresselinje3().byEllerStedsnavn().postkode().landkode()))
            .deltBosted(new DeltBostedResponseProjection().startdatoForKontrakt().sluttdatoForKontrakt()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer()));


        var personFraPdl = pdlKlient.hentPerson(query, projection);

        var fødselsdato = personFraPdl.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var dødssdato = personFraPdl.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var pdlStatus = personFraPdl.getFolkeregisterpersonstatus().stream()
            .map(Folkeregisterpersonstatus::getStatus)
            .findFirst().map(PersonstatusType::fraFregPersonstatus).orElse(PersonstatusType.UDEFINERT);
        var sivilstand = personFraPdl.getSivilstand().stream()
            .map(Sivilstand::getType)
            .map(st -> SIVSTAND_FRA_FREG.getOrDefault(st, SivilstandType.UOPPGITT))
            .findFirst().orElse(SivilstandType.UOPPGITT);
        var statsborgerskap = mapStatsborgerskap(personFraPdl.getStatsborgerskap());
        var familierelasjoner = mapFamilierelasjoner(personFraPdl.getForelderBarnRelasjon(),
            personFraPdl.getSivilstand());
        var adresser = mapAdresser(personFraPdl.getBostedsadresse(), personFraPdl.getKontaktadresse(),
            personFraPdl.getOppholdsadresse());
        var deltBosted = mapDeltBosted(personFraPdl.getDeltBosted());

        return new Personinfo.Builder()
            .medAktørId(aktørId)
            .medPersonIdent(personIdent)
            .medNavn(personFraPdl.getNavn().stream().map(PersoninfoTjeneste::mapNavn).filter(Objects::nonNull)
                .findFirst().orElse(null))
            .medFødselsdato(fødselsdato)
            .medDødsdato(dødssdato)
            .medNavBrukerKjønn(mapKjønn(personFraPdl))
            .medPersonstatusType(pdlStatus)
            .medSivilstandType(sivilstand)
            .medLandkode(statsborgerskap)
            .medRegion(Region.finnHøyestRangertRegion(List.of(statsborgerskap.getKode())))
            .medFamilierelasjon(familierelasjoner)
            .medAdresseInfoList(adresser)
            .medDeltBostedList(deltBosted)
            .build();
    }

    public Personhistorikkinfo hentPersoninfoHistorikk(AktørId aktørId, Periode periode) {
        var fom = periode.getFom();
        var tom = periode.getTom();
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());

        var projection = new PersonResponseProjection()
            .folkeregisterpersonstatus(new PersonFolkeregisterpersonstatusParametrizedInput().historikk(true),
                new FolkeregisterpersonstatusResponseProjection()
                    .forenkletStatus().status()
                    .folkeregistermetadata(new FolkeregistermetadataResponseProjection()
                        .ajourholdstidspunkt().gyldighetstidspunkt().opphoerstidspunkt()))
            .statsborgerskap(new PersonStatsborgerskapParametrizedInput().historikk(true),
                new StatsborgerskapResponseProjection().land().gyldigFraOgMed().gyldigTilOgMed())
            .bostedsadresse(new PersonBostedsadresseParametrizedInput().historikk(true),
                new BostedsadresseResponseProjection().angittFlyttedato().gyldigFraOgMed().gyldigTilOgMed()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer()
                        .husbokstav().postnummer())
                    .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId()
                        .bruksenhetsnummer().tilleggsnavn().postnummer())
                    .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer()
                        .bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade()
                        .postkode().landkode()))
            .oppholdsadresse(new PersonOppholdsadresseParametrizedInput().historikk(true),
                new OppholdsadresseResponseProjection().gyldigFraOgMed().gyldigTilOgMed()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer()
                        .husbokstav().postnummer())
                    .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId()
                        .bruksenhetsnummer().tilleggsnavn().postnummer())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer()
                        .bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade()
                        .postkode().landkode()))
            .kontaktadresse(new PersonKontaktadresseParametrizedInput().historikk(true),
                new KontaktadresseResponseProjection().type().gyldigFraOgMed().gyldigTilOgMed()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer()
                        .husbokstav().postnummer())
                    .postboksadresse(
                        new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                    .postadresseIFrittFormat(new PostadresseIFrittFormatResponseProjection().adresselinje1()
                        .adresselinje2().adresselinje3().postnummer())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer()
                        .bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade()
                        .postkode().landkode())
                    .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection()
                        .adresselinje1().adresselinje2().adresselinje3().byEllerStedsnavn().postkode()
                        .landkode()));

        var person = pdlKlient.hentPerson(query, projection);

        var personhistorikkBuilder = Personhistorikkinfo.builder().medAktørId(aktørId.getId());
        var personStatusPerioder = person.getFolkeregisterpersonstatus().stream()
            .map(PersoninfoTjeneste::mapPersonstatusHistorisk)
            .collect(Collectors.toList());
        periodiserPersonstatus(personStatusPerioder).stream()
            .filter(p -> p.getGyldighetsperiode().getTom().isAfter(fom)
                && p.getGyldighetsperiode().getFom().isBefore(tom))
            .forEach(personhistorikkBuilder::leggTil);

        person.getStatsborgerskap().stream()
            .map(PersoninfoTjeneste::mapStatsborgerskapHistorikk)
            .filter(p -> p.getGyldighetsperiode().getTom().isAfter(fom)
                && p.getGyldighetsperiode().getFom().isBefore(tom))
            .forEach(personhistorikkBuilder::leggTil);

        var adressePerioder = mapAdresserHistorikk(person.getBostedsadresse(), person.getKontaktadresse(),
            person.getOppholdsadresse());
        periodiserAdresse(adressePerioder).stream()
            .filter(p -> p.getGyldighetsperiode().getTom().isAfter(fom)
                && p.getGyldighetsperiode().getFom().isBefore(tom))
            .forEach(personhistorikkBuilder::leggTil);

        return personhistorikkBuilder.build();
    }

    public List<DeltBosted> mapDeltBosted(List<no.nav.k9.felles.integrasjon.pdl.DeltBosted> deltBostedFraPdl) {
        return deltBostedFraPdl
            .stream()
            .map(p -> new DeltBosted(
                new Periode(LocalDate.parse(p.getStartdatoForKontrakt(), DateTimeFormatter.ISO_LOCAL_DATE),
                    p.getSluttdatoForKontrakt() == null ? Tid.TIDENES_ENDE : LocalDate.parse(p.getSluttdatoForKontrakt(), DateTimeFormatter.ISO_LOCAL_DATE)),
                mapVegadresse(AdresseType.BOSTEDSADRESSE, p.getVegadresse())))
            .collect(Collectors.toList());
    }

    static List<AdressePeriode> mapAdresserHistorikk(List<Bostedsadresse> bostedsadresser,
                                                     List<Kontaktadresse> kontaktadresser, List<Oppholdsadresse> oppholdsadresser) {
        List<AdressePeriode> adresser = new ArrayList<>();
        bostedsadresser.stream().sorted(Comparator.comparing(it -> fomNullAble(it.getGyldigFraOgMed()))).forEachOrdered(b -> {
            var periode = periodeFraDates(b.getGyldigFraOgMed(), b.getGyldigTilOgMed());
            var flyttedato = b.getAngittFlyttedato() != null
                ? LocalDate.parse(b.getAngittFlyttedato(), DateTimeFormatter.ISO_LOCAL_DATE)
                : periode.getFom();
            var periode2 = flyttedato.isBefore(periode.getFom())
                ? Gyldighetsperiode.innenfor(flyttedato, periode.getTom())
                : periode;
            mapBostedadresserUkjentHvisMangler(List.of(b))
                .forEach(a -> adresser.add(mapAdresseinfoTilAdressePeriode(periode2, a)));
        });
        kontaktadresser.stream().sorted(Comparator.comparing(it -> fomNullAble(it.getGyldigFraOgMed()))).forEachOrdered(k -> {
            var periode = periodeFraDates(k.getGyldigFraOgMed(), k.getGyldigTilOgMed());
            mapKontaktadresser(List.of(k))
                .forEach(a -> adresser.add(mapAdresseinfoTilAdressePeriode(periode, a)));
        });
        oppholdsadresser.stream().sorted(Comparator.comparing(it -> fomNullAble(it.getGyldigFraOgMed()))).forEachOrdered(o -> {
            var periode = periodeFraDates(o.getGyldigFraOgMed(), o.getGyldigTilOgMed());
            mapOppholdsadresser(List.of(o))
                .forEach(a -> adresser.add(mapAdresseinfoTilAdressePeriode(periode, a)));
        });
        return adresser;
    }

    private static LocalDate fomNullAble(Date gyldigFraOgMed) {
        return gyldigFraOgMed == null ? Tid.TIDENES_BEGYNNELSE : LocalDate.ofInstant(gyldigFraOgMed.toInstant(), ZoneId.systemDefault());
    }

    private List<Adresseinfo> mapAdresser(List<Bostedsadresse> bostedsadresse, List<Kontaktadresse> kontaktadresse, List<Oppholdsadresse> oppholdsadresse) {
        List<Adresseinfo> resultat = new ArrayList<>();
        resultat.addAll(mapBostedadresser(bostedsadresse));
        resultat.addAll(mapOppholdsadresser(oppholdsadresse));
        resultat.addAll(mapKontaktadresser(kontaktadresse));
        if (resultat.isEmpty()) {
            resultat.add(mapUkjentadresse(null));
        }
        return resultat;
    }

    private static List<Adresseinfo> mapBostedadresserUkjentHvisMangler(List<Bostedsadresse> bostedsadresser) {
        List<Adresseinfo> resultat = mapBostedadresser(bostedsadresser);
        if (!resultat.isEmpty()) {
            return resultat;
        }
        return List.of(mapUkjentadresse(null));
    }

    private static List<Adresseinfo> mapBostedadresser(List<Bostedsadresse> bostedsadresser) {
        List<Adresseinfo> resultat = new ArrayList<>();
        bostedsadresser.stream().map(Bostedsadresse::getVegadresse)
            .map(a -> mapVegadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getMatrikkeladresse)
            .map(a -> mapMatrikkeladresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull)
            .forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUkjentBosted).filter(Objects::nonNull)
            .map(PersoninfoTjeneste::mapUkjentadresse).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUtenlandskAdresse)
            .map(a -> mapUtenlandskadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull)
            .forEach(resultat::add);
        return resultat;
    }

    private static List<Adresseinfo> mapOppholdsadresser(List<Oppholdsadresse> oppholdsadresser) {
        List<Adresseinfo> resultat = new ArrayList<>();
        oppholdsadresser.stream().map(Oppholdsadresse::getVegadresse)
            .map(a -> mapVegadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull)
            .forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getMatrikkeladresse)
            .map(a -> mapMatrikkeladresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull)
            .forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getUtenlandskAdresse)
            .map(a -> mapUtenlandskadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, a)).filter(Objects::nonNull)
            .forEach(resultat::add);
        return resultat;
    }

    private static List<Adresseinfo> mapKontaktadresser(List<Kontaktadresse> kontaktadresser) {
        List<Adresseinfo> resultat = new ArrayList<>();
        kontaktadresser.stream().map(Kontaktadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.POSTADRESSE, a))
            .filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostboksadresse).map(PersoninfoTjeneste::mapPostboksadresse)
            .filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostadresseIFrittFormat).map(PersoninfoTjeneste::mapFriAdresseNorsk)
            .filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresse)
            .map(a -> mapUtenlandskadresse(AdresseType.POSTADRESSE_UTLAND, a)).filter(Objects::nonNull)
            .forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresseIFrittFormat)
            .map(PersoninfoTjeneste::mapFriAdresseUtland).filter(Objects::nonNull).forEach(resultat::add);
        return resultat;
    }

    private static Adresseinfo mapVegadresse(AdresseType type, Vegadresse vegadresse) {
        if (vegadresse == null)
            return null;
        String postnummer = Optional.ofNullable(vegadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var gateadresse = vegadresse.getAdressenavn().toUpperCase()
            + hvisfinnes2(vegadresse.getHusnummer(), vegadresse.getHusbokstav());
        return Adresseinfo.builder(type)
            .medMatrikkelId(vegadresse.getMatrikkelId())
            .medAdresselinje1(gateadresse)
            .medPostNr(postnummer)
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private static Adresseinfo mapMatrikkeladresse(AdresseType type, Matrikkeladresse matrikkeladresse) {
        if (matrikkeladresse == null)
            return null;
        String postnummer = Optional.ofNullable(matrikkeladresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
            .medMatrikkelId(matrikkeladresse.getMatrikkelId())
            .medAdresselinje1(
                matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getTilleggsnavn().toUpperCase()
                    : matrikkeladresse.getBruksenhetsnummer())
            .medAdresselinje2(
                matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getBruksenhetsnummer() : null)
            .medPostNr(postnummer)
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private static Adresseinfo mapPostboksadresse(Postboksadresse postboksadresse) {
        if (postboksadresse == null)
            return null;
        String postnummer = Optional.ofNullable(postboksadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var postboks = "Postboks" + hvisfinnes(postboksadresse.getPostboks());
        return Adresseinfo.builder(AdresseType.POSTADRESSE)
            .medAdresselinje1(
                postboksadresse.getPostbokseier() != null ? postboksadresse.getPostbokseier().toUpperCase()
                    : postboks)
            .medAdresselinje2(postboksadresse.getPostbokseier() != null ? postboks : null)
            .medPostNr(postnummer)
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private static Adresseinfo mapFriAdresseNorsk(PostadresseIFrittFormat postadresse) {
        if (postadresse == null)
            return null;
        String postnummer = Optional.ofNullable(postadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(AdresseType.POSTADRESSE)
            .medAdresselinje1(
                postadresse.getAdresselinje1() != null ? postadresse.getAdresselinje1().toUpperCase() : null)
            .medAdresselinje2(
                postadresse.getAdresselinje2() != null ? postadresse.getAdresselinje2().toUpperCase() : null)
            .medAdresselinje3(
                postadresse.getAdresselinje3() != null ? postadresse.getAdresselinje3().toUpperCase() : null)
            .medPostNr(postnummer)
            .medLand(Landkoder.NOR.getKode())
            .build();
    }
}
