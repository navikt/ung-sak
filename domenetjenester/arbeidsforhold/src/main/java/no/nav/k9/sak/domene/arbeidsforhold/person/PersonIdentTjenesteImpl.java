package no.nav.k9.sak.domene.arbeidsforhold.person;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.k9.sak.domene.person.tps.PersoninfoAdapter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

@Dependent
class PersonIdentTjenesteImpl implements PersonIdentTjeneste {

    private TpsAdapterImpl tpsAdapter;
    private PersoninfoAdapter personinfoAdapter;

    public PersonIdentTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public PersonIdentTjenesteImpl(TpsAdapterImpl tpsAdapter) {
        this.tpsAdapter = tpsAdapter;
    }

    @Override
    public PersonIdent hentFnrForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr;
        funnetFnr = hentFnr(aktørId);
        if (funnetFnr.isPresent()) {
            return funnetFnr.get();
        }
        throw TpsFeilmeldinger.FACTORY.fantIkkePersonForAktørId().toException();
    }

    @Override
    public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
        return tpsAdapter.hentAktørIdForPersonIdent(fnr);
    }

    private Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return tpsAdapter.hentIdentForAktørId(aktørId);
    }

    @Override
    //TODO Skal returnere Optional<PersoninfoArbeidsgiver>, så klientene må endres
    public Optional<Personinfo> hentBrukerForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);

        Optional<PersoninfoArbeidsgiver> personinfo =  personinfoAdapter.hentPersoninfoArbeidsgiver(aktørId);

        return funnetFnr.map(fnr -> tpsAdapter.hentKjerneinformasjon(fnr, aktørId));
    }

}
