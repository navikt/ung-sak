package no.nav.k9.sak.domene.arbeidsforhold.person;

import java.util.Optional;

import no.nav.k9.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

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
