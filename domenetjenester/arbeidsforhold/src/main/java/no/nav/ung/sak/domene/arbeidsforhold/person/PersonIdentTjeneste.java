package no.nav.ung.sak.domene.arbeidsforhold.person;

import java.util.Optional;

import no.nav.ung.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

public interface PersonIdentTjeneste {

    Optional<PersoninfoArbeidsgiver> hentPersoninfoArbeidsgiver(AktørId aktørId);

    /**
     * Hent PersonIdent (FNR) for gitt aktørId.
     *
     * @throws TekniskException hvis ikke finner.
     */
    PersonIdent hentFnrForAktør(AktørId aktørId);

    Optional<AktørId> hentAktørForFnr(PersonIdent fnr);

}
