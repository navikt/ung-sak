package no.nav.k9.sak.domene.person.pdl;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.stream.Collectors.joining;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.pdl.Bostedsadresse;
import no.nav.pdl.BostedsadresseResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.FamilierelasjonResponseProjection;
import no.nav.pdl.Familierelasjonsrolle;
import no.nav.pdl.Foedsel;
import no.nav.pdl.FoedselResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.InnflyttingTilNorge;
import no.nav.pdl.InnflyttingTilNorgeResponseProjection;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Kontaktadresse;
import no.nav.pdl.KontaktadresseResponseProjection;
import no.nav.pdl.Matrikkeladresse;
import no.nav.pdl.MatrikkeladresseResponseProjection;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Opphold;
import no.nav.pdl.OppholdResponseProjection;
import no.nav.pdl.Oppholdsadresse;
import no.nav.pdl.OppholdsadresseResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.PostadresseIFrittFormat;
import no.nav.pdl.PostadresseIFrittFormatResponseProjection;
import no.nav.pdl.Postboksadresse;
import no.nav.pdl.PostboksadresseResponseProjection;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.SivilstandResponseProjection;
import no.nav.pdl.Sivilstandstype;
import no.nav.pdl.Statsborgerskap;
import no.nav.pdl.StatsborgerskapResponseProjection;
import no.nav.pdl.UkjentBosted;
import no.nav.pdl.UkjentBostedResponseProjection;
import no.nav.pdl.UtenlandskAdresse;
import no.nav.pdl.UtenlandskAdresseIFrittFormat;
import no.nav.pdl.UtenlandskAdresseIFrittFormatResponseProjection;
import no.nav.pdl.UtenlandskAdresseResponseProjection;
import no.nav.pdl.UtflyttingFraNorge;
import no.nav.pdl.UtflyttingFraNorgeResponseProjection;
import no.nav.pdl.Vegadresse;
import no.nav.pdl.VegadresseResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;

@ApplicationScoped
public class PersoninfoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PersoninfoTjeneste.class);
    private static final String HARDKODET_POSTNR = "XXXX";
    private static final String HARDKODET_POSTSTED = "UKJENT";

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
        entry(Sivilstandstype.GJENLEVENDE_PARTNER, SivilstandType.GJENLEVENDE_PARTNER)
    );

    private static final Map<Familierelasjonsrolle, RelasjonsRolleType> ROLLE_FRA_FREG_ROLLE = ofEntries(
        entry(Familierelasjonsrolle.BARN, RelasjonsRolleType.BARN),
        entry(Familierelasjonsrolle.MOR, RelasjonsRolleType.MORA),
        entry(Familierelasjonsrolle.FAR, RelasjonsRolleType.FARA),
        entry(Familierelasjonsrolle.MEDMOR, RelasjonsRolleType.MEDMOR)
    );

    private static final Map<Sivilstandstype, RelasjonsRolleType> ROLLE_FRA_FREG_STAND = ofEntries(
        entry(Sivilstandstype.GIFT, RelasjonsRolleType.EKTE),
        entry(Sivilstandstype.REGISTRERT_PARTNER, RelasjonsRolleType.REGISTRERT_PARTNER)
    );


    private PdlKlient pdlKlient;

    @SuppressWarnings("unused")
    PersoninfoTjeneste() {
        // CDI
    }

    @Inject
    public PersoninfoTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    private static String mapNavn(Navn navn) {
        if (navn.getForkortetNavn() != null)
            return navn.getForkortetNavn();
        return navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
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
            .collect(Collectors.toList());
        return alleLand.stream().anyMatch(Landkoder.NOR::equals) ? Landkoder.NOR : alleLand.stream().findFirst().orElse(Landkoder.UOPPGITT_UKJENT);
    }

    private static Set<Familierelasjon> mapFamilierelasjoner(List<no.nav.pdl.Familierelasjon> familierelasjoner, List<Sivilstand> sivilstandliste) {
        Set<Familierelasjon> relasjoner = new HashSet<>();
        // FIXME: utled samme bosted ut fra adresse

        familierelasjoner.stream()
            .map(r -> new Familierelasjon(
                    new PersonIdent(r.getRelatertPersonsIdent()),
                    mapRelasjonsrolle(r.getRelatertPersonsRolle()),
                    false
                )
            )
            .forEach(relasjoner::add);
        sivilstandliste.stream()
            .filter(rel -> Sivilstandstype.GIFT.equals(rel.getType()) || Sivilstandstype.REGISTRERT_PARTNER.equals(rel.getType()))
            .map(r -> new Familierelasjon(new PersonIdent(r.getRelatertVedSivilstand()), mapRelasjonsrolle(r.getType()), false))
            .forEach(relasjoner::add);
        return relasjoner;
    }

    private static RelasjonsRolleType mapRelasjonsrolle(Familierelasjonsrolle type) {
        return ROLLE_FRA_FREG_ROLLE.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private static RelasjonsRolleType mapRelasjonsrolle(Sivilstandstype type) {
        return ROLLE_FRA_FREG_STAND.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private static Adresseinfo mapUkjentadresse(UkjentBosted ukjentBosted) {
        return Adresseinfo.builder(AdresseType.UKJENT_ADRESSE).build();
    }

    private static Adresseinfo mapUtenlandskadresse(AdresseType type, UtenlandskAdresse utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var linje1 = hvisfinnes(utenlandskAdresse.getAdressenavnNummer()) + hvisfinnes(utenlandskAdresse.getBygningEtasjeLeilighet()) + hvisfinnes(utenlandskAdresse.getPostboksNummerNavn());
        var linje2 = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getBySted()) + hvisfinnes(utenlandskAdresse.getRegionDistriktOmraade());
        return Adresseinfo.builder(type)
            .medAdresselinje1(linje1)
            .medAdresselinje2(linje2)
            .medAdresselinje3(utenlandskAdresse.getLandkode())
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private static Adresseinfo mapFriAdresseUtland(AdresseType type, UtenlandskAdresseIFrittFormat utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var postlinje = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getByEllerStedsnavn());
        var sisteline = utenlandskAdresse.getAdresselinje3() != null ? postlinje + utenlandskAdresse.getLandkode()
            : (utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getLandkode() : null);
        return Adresseinfo.builder(type)
            .medAdresselinje1(utenlandskAdresse.getAdresselinje1())
            .medAdresselinje2(utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getAdresselinje2().toUpperCase() : postlinje)
            .medAdresselinje3(utenlandskAdresse.getAdresselinje3() != null ? utenlandskAdresse.getAdresselinje3().toUpperCase() : (utenlandskAdresse.getAdresselinje2() != null ? postlinje : utenlandskAdresse.getLandkode()))
            .medAdresselinje4(sisteline)
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private String sammenligneFamilierelasjoner(Personinfo tps, Personinfo pdl) {
        var pdlRelasjoner =
            pdl.getFamilierelasjoner()
                .stream()
                .collect(Collectors.toMap(famrel -> famrel.getPersonIdent(), famrel -> famrel));
        return tps.getFamilierelasjoner().stream()
            .map(tpsRel -> {
                    var pdlRelasjon = pdlRelasjoner.get(tpsRel.getPersonIdent());
                    if (pdlRelasjon == null) {
                        return " familierelasjon-ident-mismatch ";
                    }
                    if (!pdlRelasjon.getRelasjonsrolle().equals(tpsRel.getRelasjonsrolle())) {
                        return " familierelasjon-relasjonsrolle-mismatch ";
                    }
                    return "";
                }
            )
            .collect(joining(""));
    }

    private void logInnUtOpp(List<InnflyttingTilNorge> inn, List<UtflyttingFraNorge> ut, List<Opphold> opp) {
        String inns = inn.stream().map(InnflyttingTilNorge::getFraflyttingsland).collect(joining(", "));
        String uts = ut.stream().map(UtflyttingFraNorge::getTilflyttingsland).collect(joining(", "));
        String opps = opp.stream().map(o -> "OppholdType=" + o.getType().toString() + " Fra=" + o.getOppholdFra() + " Til=" + o.getOppholdTil())
            .collect(joining(", "));
        if (!inn.isEmpty() || !ut.isEmpty()) {
            LOG.info("K9SAK PDL full inn {} ut {}", inns, uts);
        }
        if (!opp.isEmpty()) {
            LOG.info("K9SAK PDL full opphold {}", opps);
        }
    }

    private static String hvisfinnes(Object object) {
        return object == null ? "" : " " + object.toString().trim().toUpperCase();
    }

    private static String hvisfinnes2(Object object1, Object object2) {
        if (object1 == null && object2 == null) return "";
        if (object1 != null && object2 != null)
            return " " + object1.toString().trim().toUpperCase() + object2.toString().trim().toUpperCase();
        return object2 == null ? " " + object1.toString().trim().toUpperCase() : " " + object2.toString().trim().toUpperCase();
    }

    public void hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent, Personinfo fraTPS) {
        try {
            var query = new HentPersonQueryRequest();
            query.setIdent(aktørId.getId());
            var projection = new PersonResponseProjection()
                .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
                .foedsel(new FoedselResponseProjection().foedselsdato())
                .doedsfall(new DoedsfallResponseProjection().doedsdato())
                .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status())
                .opphold(new OppholdResponseProjection().type().oppholdFra().oppholdTil())
                .innflyttingTilNorge(new InnflyttingTilNorgeResponseProjection().fraflyttingsland())
                .utflyttingFraNorge(new UtflyttingFraNorgeResponseProjection().tilflyttingsland())
                .kjoenn(new KjoennResponseProjection().kjoenn())
                .sivilstand(new SivilstandResponseProjection().relatertVedSivilstand().type())
                .statsborgerskap(new StatsborgerskapResponseProjection().land())
                .familierelasjoner(new FamilierelasjonResponseProjection().relatertPersonsRolle().relatertPersonsIdent().minRolleForPerson())
                .bostedsadresse(new BostedsadresseResponseProjection().gyldigFraOgMed().angittFlyttedato()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                    .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                    .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                .oppholdsadresse(new OppholdsadresseResponseProjection().gyldigFraOgMed()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                    .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                .kontaktadresse(new KontaktadresseResponseProjection().type().gyldigFraOgMed()
                    .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                    .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                    .postadresseIFrittFormat(new PostadresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().postnummer())
                    .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode())
                    .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().byEllerStedsnavn().postkode().landkode()));

            var person = pdlKlient.hentPerson(query, projection);

            var fødselsdato = person.getFoedsel().stream()
                .map(Foedsel::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
            var dødssdato = person.getDoedsfall().stream()
                .map(Doedsfall::getDoedsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
            var pdlStatus = person.getFolkeregisterpersonstatus().stream()
                .map(Folkeregisterpersonstatus::getStatus)
                .findFirst().map(PersonstatusType::fraFregPersonstatus).orElse(PersonstatusType.UDEFINERT);
            var sivilstand = person.getSivilstand().stream()
                .map(Sivilstand::getType)
                .map(st -> SIVSTAND_FRA_FREG.getOrDefault(st, SivilstandType.UOPPGITT))
                .findFirst().orElse(SivilstandType.UOPPGITT);
            var statsborgerskap = mapStatsborgerskap(person.getStatsborgerskap());
            var familierelasjoner = mapFamilierelasjoner(person.getFamilierelasjoner(), person.getSivilstand());
            var adresser = mapAdresser(person.getBostedsadresse(), person.getKontaktadresse(), person.getOppholdsadresse());

            var fraPDL = new Personinfo.Builder()
                .medAktørId(aktørId)
                .medPersonIdent(personIdent)
                .medNavn(person.getNavn().stream().map(PersoninfoTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse(null))
                .medFødselsdato(fødselsdato)
                .medDødsdato(dødssdato)
                .medNavBrukerKjønn(mapKjønn(person))
                .medPersonstatusType(pdlStatus)
                .medSivilstandType(sivilstand)
                .medLandkode(statsborgerskap)
                .medRegion(Region.finnHøyestRangertRegion(List.of(statsborgerskap.getKode())))
                .medFamilierelasjon(familierelasjoner)
                .medAdresseInfoList(adresser)
                .build();
            logInnUtOpp(person.getInnflyttingTilNorge(), person.getUtflyttingFraNorge(), person.getOpphold());
            if (erLike(fraPDL, fraTPS)) {
                LOG.info("K9SAK PDL full personinfo: like svar");
            } else {
                LOG.info("K9SAK PDL full personinfo: avvik {}", finnAvvik(fraTPS, fraPDL));
            }
        } catch (Exception e) {
            LOG.info("K9SAK PDL full personinfo: error", e);
        }
    }

    private boolean erLike(Personinfo pdl, Personinfo tps) {
        if (tps == null && pdl == null) return true;
        if (pdl == null || tps == null || tps.getClass() != pdl.getClass()) return false;
        var likerels = pdl.getFamilierelasjoner().size() == tps.getFamilierelasjoner().size() &&
            pdl.getFamilierelasjoner().containsAll(tps.getFamilierelasjoner());
        var likeadresser = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() &&
            pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList());
        return Objects.equals(pdl.getNavn(), tps.getNavn()) &&
            Objects.equals(pdl.getFødselsdato(), tps.getFødselsdato()) &&
            Objects.equals(pdl.getDødsdato(), tps.getDødsdato()) &&
            pdl.getPersonstatus() == tps.getPersonstatus() &&
            pdl.getKjønn() == tps.getKjønn() &&
            likerels &&
            likeadresser &&
            pdl.getRegion() == tps.getRegion() &&
            pdl.getLandkode() == tps.getLandkode() &&
            pdl.getSivilstandType() == tps.getSivilstandType();
    }

    private String finnAvvik(Personinfo tps, Personinfo pdl) {
        String navn = Objects.equals(tps.getNavn(), pdl.getNavn()) ? "" : " navn ";
        String kjonn = Objects.equals(tps.getKjønn(), pdl.getKjønn()) ? "" : " kjønn ";
        String fdato = Objects.equals(tps.getFødselsdato(), pdl.getFødselsdato()) ? "" : " fødsel ";
        String ddato = Objects.equals(tps.getDødsdato(), pdl.getDødsdato()) ? "" : " død ";
        String status = Objects.equals(tps.getPersonstatus(), pdl.getPersonstatus()) ? "" : " status " + tps.getPersonstatus().getKode() + " PDL " + pdl.getPersonstatus().getKode();
        String sivstand = Objects.equals(tps.getSivilstandType(), pdl.getSivilstandType()) ? "" : " sivilst " + tps.getSivilstandType().getKode() + " PDL " + pdl.getSivilstandType().getKode();
        String land = Objects.equals(tps.getLandkode(), pdl.getLandkode()) ? "" : " land " + tps.getLandkode().getKode() + " PDL " + pdl.getLandkode().getKode();
        String region = Objects.equals(tps.getRegion(), pdl.getRegion()) ? "" : " region " + tps.getRegion().getKode() + " PDL " + pdl.getRegion().getKode();

        int famRelPdlSize = pdl.getFamilierelasjoner().size();
        int famRelTpsSize = tps.getFamilierelasjoner().size();
        boolean famRelSammeAntall = famRelPdlSize == famRelTpsSize;
        String famRelAntall = famRelSammeAntall ? "" : String.format(" antall familierelasjon:  PDL=%s  TPS=%s", famRelPdlSize, famRelTpsSize);
        String famRelAvvik = famRelSammeAntall ? sammenligneFamilierelasjoner(tps, pdl) : "";

        String adresse = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse " + tps.getAdresseInfoList().stream().map(Adresseinfo::getGjeldendePostadresseType).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getGjeldendePostadresseType).collect(Collectors.toList());
        String adresse2 = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse2 " + tps.getAdresseInfoList().stream().map(Adresseinfo::getPostNr).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getPostNr).collect(Collectors.toList());
        String adresse3 = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse3 " + tps.getAdresseInfoList().stream().map(Adresseinfo::getLand).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getLand).collect(Collectors.toList());

        return "Avvik" + navn + kjonn + fdato + ddato + status + sivstand + land + region + famRelAntall + famRelAvvik + adresse + adresse2 + adresse3;
    }

    private List<Adresseinfo> mapAdresser(List<Bostedsadresse> bostedsadresser, List<Kontaktadresse> kontaktadresser, List<Oppholdsadresse> oppholdsadresser) {
        List<Adresseinfo> resultat = new ArrayList<>();
        bostedsadresser.stream().map(Bostedsadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getMatrikkeladresse).map(a -> mapMatrikkeladresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUkjentBosted).filter(Objects::nonNull).map(PersoninfoTjeneste::mapUkjentadresse).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);

        oppholdsadresser.stream().map(Oppholdsadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull).forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getMatrikkeladresse).map(a -> mapMatrikkeladresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull).forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);

        kontaktadresser.stream().map(Kontaktadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostboksadresse).map(a -> mapPostboksadresse(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostadresseIFrittFormat).map(a -> mapFriAdresseNorsk(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresseIFrittFormat).map(a -> mapFriAdresseUtland(AdresseType.POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);
        if (resultat.isEmpty()) {
            resultat.add(mapUkjentadresse(null));
        }
        return resultat;
    }

    private Adresseinfo mapVegadresse(AdresseType type, Vegadresse vegadresse) {
        if (vegadresse == null)
            return null;
        String postnummer = Optional.ofNullable(vegadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var gateadresse = vegadresse.getAdressenavn().toUpperCase() + hvisfinnes2(vegadresse.getHusnummer(), vegadresse.getHusbokstav());
        return Adresseinfo.builder(type)
            .medMatrikkelId(vegadresse.getMatrikkelId())
            .medAdresselinje1(gateadresse)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapMatrikkeladresse(AdresseType type, Matrikkeladresse matrikkeladresse) {
        if (matrikkeladresse == null)
            return null;
        String postnummer = Optional.ofNullable(matrikkeladresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
            .medMatrikkelId(matrikkeladresse.getMatrikkelId())
            .medAdresselinje1(matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getTilleggsnavn().toUpperCase() : matrikkeladresse.getBruksenhetsnummer())
            .medAdresselinje2(matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getBruksenhetsnummer() : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapPostboksadresse(AdresseType type, Postboksadresse postboksadresse) {
        if (postboksadresse == null)
            return null;
        String postnummer = Optional.ofNullable(postboksadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var postboks = "Postboks" + hvisfinnes(postboksadresse.getPostboks());
        return Adresseinfo.builder(type)
            .medAdresselinje1(postboksadresse.getPostbokseier() != null ? postboksadresse.getPostbokseier().toUpperCase() : postboks)
            .medAdresselinje2(postboksadresse.getPostbokseier() != null ? postboks : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapFriAdresseNorsk(AdresseType type, PostadresseIFrittFormat postadresse) {
        if (postadresse == null)
            return null;
        String postnummer = Optional.ofNullable(postadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
            .medAdresselinje1(postadresse.getAdresselinje1() != null ? postadresse.getAdresselinje1().toUpperCase() : null)
            .medAdresselinje2(postadresse.getAdresselinje2() != null ? postadresse.getAdresselinje2().toUpperCase() : null)
            .medAdresselinje3(postadresse.getAdresselinje3() != null ? postadresse.getAdresselinje3().toUpperCase() : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private String tilPoststed(String postnummer) {
        if (HARDKODET_POSTNR.equals(postnummer)) {
            return HARDKODET_POSTSTED;
        }
        //TODO Trenger vi poststed? I såfall må det hentes fra et sted og hvor?
        //return poststedKodeverkRepository.finnPoststed(postnummer).map(Poststed::getPoststednavn).orElse(HARDKODET_POSTSTED);
        return postnummer;
    }


}
