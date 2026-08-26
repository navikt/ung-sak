package no.nav.ung.sak.domene.arbeidsforhold.person;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.typer.AktørId;

import java.util.Optional;

@Dependent
class PersonIdentTjenesteImpl implements PersonIdentTjeneste {

    private PersoninfoAdapter personinfoAdapter;

    @Inject
    public PersonIdentTjenesteImpl(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    //TODO Vurder om denne metoden bør flyttes til annen tjeneste eller om denne tjenesten bør endre navn
    @Override
    public Optional<PersoninfoArbeidsgiver> hentPersoninfoArbeidsgiver(AktørId aktørId, FagsakYtelseType ytelseType) {
        return personinfoAdapter.hentPersoninfoArbeidsgiver(aktørId, ytelseType);
    }
}
