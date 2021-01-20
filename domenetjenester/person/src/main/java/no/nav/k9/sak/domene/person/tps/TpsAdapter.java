package no.nav.k9.sak.domene.person.tps;

import java.util.List;
import java.util.Optional;

import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;

public interface TpsAdapter {

    Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent);

    Optional<PersonIdent> hentIdentForAktørId(AktørId aktørId);

    Personinfo hentKjerneinformasjon(PersonIdent personIdent, AktørId aktørId);

    Personhistorikkinfo hentPersonhistorikk(AktørId aktørId, Periode periode);

    Adresseinfo hentAdresseinformasjon(PersonIdent personIdent);

    /**
     * Brukes til å hente behandlende enhet / diskresjonskode gitt et fnr.
     */
    GeografiskTilknytning hentGeografiskTilknytning(PersonIdent personIdent);

    List<GeografiskTilknytning> hentDiskresjonskoderForFamilierelasjoner(PersonIdent personIdent);

    PersoninfoBasis hentKjerneinformasjonBasis(PersonIdent fnr, AktørId aktørId);
}
