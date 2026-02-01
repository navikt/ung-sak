package no.nav.ung.sak.domene.person.pdl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.ung.kodeverk.person.Diskresjonskode;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class PersonBasisTjeneste {

    private Pdl pdlKlient;
    private boolean isProd = Environment.current().isProd();

    PersonBasisTjeneste() {
        // CDI
    }

    @Inject
    public PersonBasisTjeneste(Pdl pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    private static String mapNavn(Navn navn) {
        return navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }

    public PersoninfoBasis hentBasisPersoninfo(AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato())
            .kjoenn(new KjoennResponseProjection().kjoenn())
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(query, projection, List.of(Behandlingsnummer.UNGDOMSYTELSEN));

        var fødselsdato = person.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElseGet(() -> isProd ? null : LocalDate.now().minusDays(1));
        var dødsdato = person.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);

        return new PersoninfoBasis.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
            .medNavn(person.getNavn().stream().map(PersonBasisTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElseGet(() -> isProd ? null : "Navnløs i Folkeregister"))
            .medFødselsdato(fødselsdato)
            .medDødsdato(dødsdato)
            .medDiskresjonsKode(getDiskresjonskode(person))
            .medNavBrukerKjønn(mapKjønn(person))
            .build();
    }

    private String getDiskresjonskode(Person person) {
        var kode = person.getAdressebeskyttelse().stream()
            .map(Adressebeskyttelse::getGradering)
            .filter(g -> !AdressebeskyttelseGradering.UGRADERT.equals(g))
            .findFirst().orElse(null);
        if (AdressebeskyttelseGradering.STRENGT_FORTROLIG.equals(kode) || AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.equals(kode))
            return Diskresjonskode.KODE6.getKode();
        return AdressebeskyttelseGradering.FORTROLIG.equals(kode) ? Diskresjonskode.KODE7.getKode() : null;
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

    public PersoninfoArbeidsgiver hentPersoninfoArbeidsgiver(AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());
        var personFraPDL = pdlKlient.hentPerson(query, projection, List.of(Behandlingsnummer.UNGDOMSYTELSEN));

        var fødselsdato = personFraPDL.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst()
            .map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);

        return new PersoninfoArbeidsgiver.Builder()
            .medAktørId(aktørId)
            .medPersonIdent(personIdent)
            .medNavn(personFraPDL.getNavn().stream().map(PersonBasisTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse(null))
            .medFødselsdato(fødselsdato)
            .bygg();
    }
}
