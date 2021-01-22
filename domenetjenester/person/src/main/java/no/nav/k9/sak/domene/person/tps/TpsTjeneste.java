package no.nav.k9.sak.domene.person.tps;

import java.util.List;
import java.util.Optional;

import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
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

    List<GeografiskTilknytning> hentDiskresjonskoderForFamilierelasjoner(PersonIdent fnr);

    Optional<PersonIdent> hentFnr(AktørId aktørId);

    Adresseinfo hentAdresseinformasjon(PersonIdent personIdent);
}
