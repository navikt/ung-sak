package no.nav.k9.sak.domene.person.tps;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.ws.soap.SOAPFaultException;

import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;

@ApplicationScoped
public class PersoninfoAdapter {

    private TpsAdapter tpsAdapter;
    private PersonBasisTjeneste personBasisTjeneste;

    public PersoninfoAdapter() {
        // for CDI proxy
    }

    @Inject
    public PersoninfoAdapter(TpsAdapter tpsAdapter, PersonBasisTjeneste personBasisTjeneste) {
        this.tpsAdapter = tpsAdapter;
        this.personBasisTjeneste = personBasisTjeneste;
    }

    public Personinfo innhentSaksopplysningerForSøker(AktørId aktørId) {
        return hentKjerneinformasjon(aktørId);
    }

    public Optional<Personinfo> innhentSaksopplysningerForEktefelle(Optional<AktørId> aktørId) {
        return aktørId.map(this::hentKjerneinformasjon);
    }

    public Optional<Personinfo> innhentSaksopplysninger(PersonIdent personIdent) {
        Optional<AktørId> aktørId = tpsAdapter.hentAktørIdForPersonIdent(personIdent);

        if (aktørId.isPresent()) {
            return hentKjerneinformasjonForBarn(aktørId.get(), personIdent);
        } else {
            return Optional.empty();
        }
    }

    public Personhistorikkinfo innhentPersonopplysningerHistorikk(AktørId aktørId, Periode periode) {
        return tpsAdapter.hentPersonhistorikk(aktørId, periode);
    }

    /**
     * Henter PersonInfo for barn, gitt at det ikke er FDAT nummer (sjekkes på format av PersonIdent, evt. ved feilhåndtering fra TPS). Hvis FDAT nummer returneres {@link Optional#empty()}
     */
    public Optional<Personinfo> innhentSaksopplysningerForBarn(PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        Optional<AktørId> optAktørId = tpsAdapter.hentAktørIdForPersonIdent(personIdent);
        if (optAktørId.isPresent()) {
            return hentKjerneinformasjonForBarn(optAktørId.get(), personIdent);
        }
        return Optional.empty();
    }

    public Adresseinfo innhentAdresseopplysningerForDokumentsending(AktørId aktørId) {
        Optional<PersonIdent> optFnr = tpsAdapter.hentIdentForAktørId(aktørId);
        return optFnr.map(personIdent -> tpsAdapter.hentAdresseinformasjon(personIdent)).orElse(null);
    }

    private Optional<Personinfo> hentKjerneinformasjonForBarn(AktørId aktørId, PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                hentKjerneinformasjon(aktørId, personIdent)
            );
            // TODO Lag en skikkelig fiks på dette
            //Her sorterer vi ut dødfødte barn
        } catch (SOAPFaultException e) {
            if (e.getCause().getMessage().contains("status: S610006F")) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private Personinfo hentKjerneinformasjon(AktørId aktørId) {
        Optional<PersonIdent> personIdent = tpsAdapter.hentIdentForAktørId(aktørId);
        return personIdent.map(ident -> hentKjerneinformasjon(aktørId, ident)).orElse(null);
        //FIXME Humle returner Optional
    }

    private Personinfo hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent) {
        return tpsAdapter.hentKjerneinformasjon(personIdent, aktørId);
    }

    private Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return tpsAdapter.hentIdentForAktørId(aktørId);
    }

    public Optional<PersoninfoArbeidsgiver> hentPersoninfoArbeidsgiver(AktørId aktørId) {
        Optional<PersonIdent> personIdent = hentFnr(aktørId);
        Optional<PersoninfoArbeidsgiver> personinfoArbeidsgiver = personIdent.map(i -> tpsAdapter.hentKjerneinformasjonBasis(i, aktørId))
            .map(p ->
                new PersoninfoArbeidsgiver.Builder()
                    .medAktørId(aktørId)
                    .medPersonIdent(p.getPersonIdent())
                    .medNavn(p.getNavn())
                    .medFødselsdato(p.getFødselsdato())
                    .bygg()
            );

        // Sammnligner det som hentes fra tps og pdl
        personinfoArbeidsgiver.ifPresent(p -> personBasisTjeneste.hentOgSjekkPersoninfoArbeidsgiverFraPDL(aktørId, p.getPersonIdent(), p));

        return personinfoArbeidsgiver;
    }
}
