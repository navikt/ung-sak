package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.personopplysning.debug;

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
public class DebugPersoninfoTjeneste {

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

    DebugPersoninfoTjeneste() {
        // CDI
    }

    @Inject
    public DebugPersoninfoTjeneste(PdlKlient pdlKlient) {
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

    private static RelasjonsRolleType mapRelasjonsrolle(Sivilstandstype type) {
        return ROLLE_FRA_FREG_STAND.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    public Personinfo hentKjerneinformasjon(List<String> dumpinnhold, AktørId aktørId, PersonIdent personIdent) {

        var query = new HentPersonQueryRequest();
        query.setIdent(personIdent.getIdent());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato())
            .forelderBarnRelasjon(
                new ForelderBarnRelasjonResponseProjection()
                .relatertPersonsRolle()
                .relatertPersonsIdent()
                .minRolleForPerson()
            );


        var personFraPdl = pdlKlient.hentPerson(query, projection, List.of(Behandlingsnummer.UNGDOMSYTELSEN));

        dumpinnhold.add("pdl-kjerneinfo-query for " + aktørId.getAktørId() + ": " + PdlKallDump.toJson(query));
        dumpinnhold.add("pdl-kjerneinfo-projection for " + aktørId.getAktørId() + ": " + PdlKallDump.toJson(projection));

        var person = pdlKlient.hentPerson(query, projection, List.of(Behandlingsnummer.UNGDOMSYTELSEN));

        dumpinnhold.add("pdl-kjerneinfo-svar for " + aktørId.getAktørId() + ": " + PdlKallDump.toJson(person));

        var fødselsdato = personFraPdl.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var dødssdato = personFraPdl.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var familierelasjoner = mapFamilierelasjoner(personFraPdl.getForelderBarnRelasjon());

        Personinfo build = new Personinfo.Builder()
            .medAktørId(aktørId)
            .medPersonIdent(personIdent)
            .medNavn(personFraPdl.getNavn().stream().map(DebugPersoninfoTjeneste::mapNavn).filter(Objects::nonNull)
                .findFirst().orElse(null))
            .medFødselsdato(fødselsdato)
            .medDødsdato(dødssdato)
            .medFamilierelasjon(familierelasjoner)
            .build();

        dumpinnhold.add("personinfo for " + aktørId.getAktørId() + ": " + PdlKallDump.toJson(build));

        return build;
    }
}
