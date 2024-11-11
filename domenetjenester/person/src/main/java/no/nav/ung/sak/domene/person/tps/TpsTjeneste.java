package no.nav.ung.sak.domene.person.tps;

import java.util.Optional;

import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
public interface TpsTjeneste {

    Optional<Personinfo> hentBrukerForAktør(AktørId aktørId);

    /**
     * Hent PersonIdent (FNR) for gitt aktørId.
     *
     * @throws TekniskException hvis ikke finner.
     */
    PersonIdent hentFnrForAktør(AktørId aktørId);

    Optional<Personinfo> hentBrukerForFnr(PersonIdent fnr);

    Optional<AktørId> hentAktørForFnr(PersonIdent fnr);

    Optional<String> hentDiskresjonskodeForAktør(PersonIdent fnr);

    GeografiskTilknytning hentGeografiskTilknytning(PersonIdent fnr);

    Optional<PersonIdent> hentFnr(AktørId aktørId);

}
