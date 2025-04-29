package no.nav.ung.sak.domene.person.pdl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

@ApplicationScoped
public class PersoninfoTjeneste {

    private static final String HARDKODET_POSTNR = "XXXX";

    private static final Set<Sivilstandstype> JURIDISK_GIFT = Set.of(Sivilstandstype.GIFT, Sivilstandstype.SEPARERT,
        Sivilstandstype.REGISTRERT_PARTNER, Sivilstandstype.SEPARERT_PARTNER);

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

    private static String mapNavn(Navn navn) {
        return navn.getEtternavn() + " " + navn.getFornavn()
            + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }

    private static Set<Familierelasjon> mapFamilierelasjoner(
        List<no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjon> forelderBarnRelasjoner) {
        Set<Familierelasjon> relasjoner = new HashSet<>();

        forelderBarnRelasjoner.stream()
            .filter(r -> r.getRelatertPersonsIdent() != null)
            .map(r -> new Familierelasjon(
                new PersonIdent(r.getRelatertPersonsIdent()),
                mapRelasjonsrolle(r.getRelatertPersonsRolle()),
                mapRelasjonsrolle(r.getMinRolleForPerson()))
            )
            .forEach(relasjoner::add);
        return relasjoner;
    }

    private static RelasjonsRolleType mapRelasjonsrolle(ForelderBarnRelasjonRolle type) {
        return ROLLE_FRA_FREG_ROLLE.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    public Personinfo hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent) {

        var query = new HentPersonQueryRequest();
        query.setIdent(personIdent.getIdent());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato())
            .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status())
            .forelderBarnRelasjon(
                new ForelderBarnRelasjonResponseProjection()
                    .relatertPersonsRolle()
                    .relatertPersonsIdent()
                    .minRolleForPerson()
            );

        var personFraPdl = pdlKlient.hentPerson(query, projection);

        var fødselsdato = personFraPdl.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);

        var dødssdato = personFraPdl.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);

        var familierelasjoner = mapFamilierelasjoner(personFraPdl.getForelderBarnRelasjon());

        return new Personinfo.Builder()
            .medAktørId(aktørId)
            .medPersonIdent(personIdent)
            .medNavn(personFraPdl.getNavn().stream().map(PersoninfoTjeneste::mapNavn).filter(Objects::nonNull)
                .findFirst().orElse(null))
            .medFødselsdato(fødselsdato)
            .medDødsdato(dødssdato)
            .medFamilierelasjon(familierelasjoner)
            .build();
    }
}
