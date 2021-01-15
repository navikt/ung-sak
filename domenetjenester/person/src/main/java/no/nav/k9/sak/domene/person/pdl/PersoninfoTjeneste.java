package no.nav.k9.sak.domene.person.pdl;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
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
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Opphold;
import no.nav.pdl.OppholdResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.SivilstandResponseProjection;
import no.nav.pdl.Sivilstandstype;
import no.nav.pdl.Statsborgerskap;
import no.nav.pdl.StatsborgerskapResponseProjection;
import no.nav.pdl.UtflyttingFraNorge;
import no.nav.pdl.UtflyttingFraNorgeResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;

@ApplicationScoped
public class PersoninfoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PersoninfoTjeneste.class);

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
                .familierelasjoner(new FamilierelasjonResponseProjection().relatertPersonsRolle().relatertPersonsIdent().minRolleForPerson());

            var person = pdlKlient.hentPerson(query, projection, Tema.OMS);

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
        return Objects.equals(pdl.getNavn(), tps.getNavn()) &&
            Objects.equals(pdl.getFødselsdato(), tps.getFødselsdato()) &&
            Objects.equals(pdl.getDødsdato(), tps.getDødsdato()) &&
            pdl.getPersonstatus() == tps.getPersonstatus() &&
            pdl.getKjønn() == tps.getKjønn() &&
            likerels &&
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
        String frel = pdl.getFamilierelasjoner().size() == tps.getFamilierelasjoner().size() && pdl.getFamilierelasjoner().containsAll(tps.getFamilierelasjoner()) ?
            "" : " famrel ";
        return "Avvik" + navn + kjonn + fdato + ddato + status + sivstand + land + region + frel;
    }

    private void logInnUtOpp(List<InnflyttingTilNorge> inn, List<UtflyttingFraNorge> ut, List<Opphold> opp) {
        String inns = inn.stream().map(InnflyttingTilNorge::getFraflyttingsland).collect(Collectors.joining(", "));
        String uts = ut.stream().map(UtflyttingFraNorge::getTilflyttingsland).collect(Collectors.joining(", "));
        String opps = opp.stream().map(o -> "OppholdType=" + o.getType().toString() + " Fra=" + o.getOppholdFra() + " Til=" + o.getOppholdTil())
            .collect(Collectors.joining(", "));
        if (!inn.isEmpty() || !ut.isEmpty()) {
            LOG.info("FPSAK PDL FULL inn {} ut {}", inns, uts);
        }
        if (!opp.isEmpty()) {
            LOG.info("FPSAK PDL FULL opphold {}", opps);
        }
    }

}
