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

    private static String mapNavn(Navn navn) {
        if (navn.getForkortetNavn() != null)
            return navn.getForkortetNavn();
        return navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }

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
}
