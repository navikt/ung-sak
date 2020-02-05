package no.nav.foreldrepenger.domene.arbeidsforhold.person;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

public interface PersonIdentTjeneste {

    Optional<Personinfo> hentBrukerForAktør(AktørId aktørId);

    /**
     * Hent PersonIdent (FNR) for gitt aktørId.
     *
     * @throws TekniskException hvis ikke finner.
     */
    PersonIdent hentFnrForAktør(AktørId aktørId);

    Optional<AktørId> hentAktørForFnr(PersonIdent fnr);

}
