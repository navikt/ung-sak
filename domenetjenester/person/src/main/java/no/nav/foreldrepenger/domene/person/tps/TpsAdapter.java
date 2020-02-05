package no.nav.foreldrepenger.domene.person.tps;

import java.util.List;
import java.util.Optional;

import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

public interface TpsAdapter {

    Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent);

    Optional<PersonIdent> hentIdentForAktørId(AktørId aktørId);

    Personinfo hentKjerneinformasjon(PersonIdent personIdent, AktørId aktørId);

    Personhistorikkinfo hentPersonhistorikk(AktørId aktørId, Interval periode);

    Adresseinfo hentAdresseinformasjon(PersonIdent personIdent);

    /**
     * Brukes til å hente behandlende enhet / diskresjonskode gitt et fnr.
     */
    GeografiskTilknytning hentGeografiskTilknytning(PersonIdent personIdent);

    List<GeografiskTilknytning> hentDiskresjonskoderForFamilierelasjoner(PersonIdent personIdent);

    List<FødtBarnInfo> hentFødteBarn(AktørId aktørId);
}
