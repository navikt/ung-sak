package no.nav.k9.sak.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.pdl.Foedsel;
import no.nav.pdl.FoedselResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class PersonBasisTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PersonBasisTjeneste.class);

    private PdlKlient pdlKlient;
    private boolean isProd = Environment.current().isProd();

    PersonBasisTjeneste() {
        // CDI
    }

    @Inject
    public PersonBasisTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

//    public PersoninfoBasis hentBasisPersoninfo(AktørId aktørId, PersonIdent personIdent) {
//        var query = new HentPersonQueryRequest();
//        query.setIdent(aktørId.getId());
//        var projection = new PersonResponseProjection()
//            .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
//            .foedsel(new FoedselResponseProjection().foedselsdato())
//            .doedsfall(new DoedsfallResponseProjection().doedsdato())
//            .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().status())
//            .kjoenn(new KjoennResponseProjection().kjoenn())
//            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
//
//        var person = pdlKlient.hentPerson(query, projection, Tema.FOR);
//
//        var fødselsdato = person.getFoedsel().stream()
//            .map(Foedsel::getFoedselsdato)
//            .filter(Objects::nonNull)
//            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElseGet(() -> isProd ? null : LocalDate.now().minusDays(1));
//        var dødsdato = person.getDoedsfall().stream()
//            .map(Doedsfall::getDoedsdato)
//            .filter(Objects::nonNull)
//            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
//        var pdlStatus = person.getFolkeregisterpersonstatus().stream()
//            .map(Folkeregisterpersonstatus::getStatus)
//            .findFirst().map(PersonstatusType::fraFregPersonstatus).orElse(PersonstatusType.UDEFINERT);
//        return new PersoninfoBasis.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
//            .medNavn(person.getNavn().stream().map(PersonBasisTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElseGet(() -> isProd ? null : "Navnløs i Folkeregister"))
//            .medFødselsdato(fødselsdato)
//            .medDødsdato(dødsdato)
//            .medDiskresjonsKode(getDiskresjonskode(person))
//            .medNavBrukerKjønn(mapKjønn(person))
//            .medPersonstatusType(pdlStatus)
//            .build();
//    }

    private static String mapNavn(Navn navn) {
        if (navn.getForkortetNavn() != null)
            return navn.getForkortetNavn();
        return navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }

//    public Optional<PersoninfoKjønn> hentKjønnPersoninfo(AktørId aktørId) {
//        var query = new HentPersonQueryRequest();
//        query.setIdent(aktørId.getId());
//        var projection = new PersonResponseProjection()
//            .kjoenn(new KjoennResponseProjection().kjoenn());
//
//        var person = pdlKlient.hentPerson(query, projection, Tema.FOR);
//
//        var kjønn = new PersoninfoKjønn.Builder().medAktørId(aktørId)
//            .medNavBrukerKjønn(mapKjønn(person))
//            .build();
//        return person.getKjoenn().isEmpty() ? Optional.empty() : Optional.of(kjønn);
//    }


//    private String getDiskresjonskode(Person person) {
//        var kode = person.getAdressebeskyttelse().stream()
//                .map(Adressebeskyttelse::getGradering)
//                .filter(g -> !AdressebeskyttelseGradering.UGRADERT.equals(g))
//                .findFirst().orElse(null);
//        if (AdressebeskyttelseGradering.STRENGT_FORTROLIG.equals(kode) || AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.equals(kode))
//            return Diskresjonskode.KODE6.getKode();
//        return AdressebeskyttelseGradering.FORTROLIG.equals(kode) ? Diskresjonskode.KODE7.getKode() : null;
//    }

    public void hentOgSjekkPersoninfoArbeidsgiverFraPDL(AktørId aktørId, PersonIdent personIdent, PersoninfoArbeidsgiver fraTPS) {
        try {
            var query = new HentPersonQueryRequest();
            query.setIdent(aktørId.getId());
            var projection = new PersonResponseProjection()
                .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
                .foedsel(new FoedselResponseProjection().foedselsdato());
            var personFraPDL = pdlKlient.hentPerson(query, projection, Tema.OMS); // K9-sak spør som "omsorgsbruker". Vurder å lage konstant.

            var fødselsdato = personFraPDL.getFoedsel().stream()
                .map(Foedsel::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst()
                .map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);

            var fraPDL =
                new PersoninfoArbeidsgiver.Builder()
                    .medAktørId(aktørId)
                    .medPersonIdent(personIdent)
                    .medNavn(personFraPDL.getNavn().stream().map(PersonBasisTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse(null))
                    .medFødselsdato(fødselsdato)
                    .bygg();

            if (Objects.equals(fraPDL, fraTPS)) {
                LOG.info("K9-SAK TPSvsPDL PersoninfoArbeidsgiver: like svar");
            } else {
                LOG.info("K9-SAK TPSvsPDL PersoninfoArbeidsgiver: avvik");
            }
        } catch (Exception e) {
            LOG.info("K9-SAK TPSvsPDL PersoninfoArbeidsgiver error", e);
        }
    }

//    private static NavBrukerKjønn mapKjønn(Person person) {
//        var kode = person.getKjoenn().stream()
//            .map(Kjoenn::getKjoenn)
//            .filter(Objects::nonNull)
//            .findFirst().orElse(KjoennType.UKJENT);
//        if (KjoennType.MANN.equals(kode))
//            return NavBrukerKjønn.MANN;
//        return KjoennType.KVINNE.equals(kode) ? NavBrukerKjønn.KVINNE : NavBrukerKjønn.UDEFINERT;
//    }

}
