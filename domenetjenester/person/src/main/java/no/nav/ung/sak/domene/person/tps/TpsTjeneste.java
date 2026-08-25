package no.nav.ung.sak.domene.person.tps;

import java.util.Optional;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
public interface TpsTjeneste {

    Optional<Personinfo> hentBrukerForAktør(AktørId aktørId, FagsakYtelseType ytelseType);

    /**
     * Hent PersonIdent (FNR) for gitt aktørId.
     *
     * @throws TekniskException hvis ikke finner.
     */
    PersonIdent hentFnrForAktør(AktørId aktørId);

    Optional<Personinfo> hentBrukerForFnr(PersonIdent fnr, FagsakYtelseType ytelseType);

    Optional<String> hentDiskresjonskodeForAktør(PersonIdent fnr, FagsakYtelseType ytelseType);

    GeografiskTilknytning hentGeografiskTilknytning(PersonIdent fnr, FagsakYtelseType ytelseType);

    Optional<PersonIdent> hentFnr(AktørId aktørId);

}
