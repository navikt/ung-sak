package no.nav.k9.sak.domene.arbeidsforhold.person;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

@Dependent
class PersonIdentTjenesteImpl implements PersonIdentTjeneste {

    private PersoninfoAdapter personinfoAdapter;
    private AktørTjeneste aktørTjeneste;

    public PersonIdentTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public PersonIdentTjenesteImpl(PersoninfoAdapter personinfoAdapter, AktørTjeneste aktørTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.aktørTjeneste = aktørTjeneste;
    }

    @Override
    public PersonIdent hentFnrForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr;
        funnetFnr = aktørTjeneste.hentPersonIdentForAktørId(aktørId);
        if (funnetFnr.isPresent()) {
            return funnetFnr.get();
        }
        throw PersonIdentFeilmeldinger.FACTORY.fantIkkePersonForAktørId().toException();
    }

    @Override
    public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
        if (fnr.erFdatNummer()) {
            // har ikke tildelt personnr
            return Optional.empty();
        }
            return aktørTjeneste.hentAktørIdForPersonIdent(fnr);
    }

    //TODO Vurder om denne metoden bør flyttes til annen tjeneste eller om denne tjenesten bør endre navn
    @Override
    public Optional<PersoninfoArbeidsgiver> hentPersoninfoArbeidsgiver(AktørId aktørId) {
        return personinfoAdapter.hentPersoninfoArbeidsgiver(aktørId);
    }
}
