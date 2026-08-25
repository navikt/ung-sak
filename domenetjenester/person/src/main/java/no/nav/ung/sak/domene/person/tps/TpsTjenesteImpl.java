package no.nav.ung.sak.domene.person.tps;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.Diskresjonskode;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@Dependent
public class TpsTjenesteImpl implements TpsTjeneste {

    private PersoninfoAdapter personinfoAdapter;

    public TpsTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public TpsTjenesteImpl(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    @Override
    public Optional<Personinfo> hentBrukerForFnr(PersonIdent fnr, FagsakYtelseType ytelseType) {
        if (fnr.erFdatNummer()) {
            return Optional.empty();
        }
        Optional<AktørId> aktørId = personinfoAdapter.hentAktørIdForPersonIdent(fnr);
        if (aktørId.isEmpty()) {
            return Optional.empty();
        }
        Personinfo personinfo = personinfoAdapter.hentKjerneinformasjon(aktørId.get(), ytelseType);
        return Optional.ofNullable(personinfo);
    }

    @Override
    public PersonIdent hentFnrForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr;
        funnetFnr = hentFnr(aktørId);
        if (funnetFnr.isPresent()) {
            return funnetFnr.get();
        }
        throw TpsFeilmeldinger.FACTORY.fantIkkePersonForAktørId().toException();
    }

    @Override
    public Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return personinfoAdapter.hentIdentForAktørId(aktørId);
    }

    @Override
    public Optional<Personinfo> hentBrukerForAktør(AktørId aktørId, FagsakYtelseType ytelseType) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        return funnetFnr.map(fnr -> personinfoAdapter.hentKjerneinformasjon(aktørId, fnr, ytelseType));
    }

    @Override
    public Optional<String> hentDiskresjonskodeForAktør(PersonIdent fnr, FagsakYtelseType ytelseType) {
        if (fnr.erFdatNummer()) {
            return Optional.empty();
        }
        return Optional.ofNullable(hentGeografiskTilknytning(fnr, ytelseType).getDiskresjonskode()).map(Diskresjonskode::getKode);
    }

    @Override
    public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent fnr, FagsakYtelseType ytelseType) {
        return personinfoAdapter.hentGeografiskTilknytning(fnr, ytelseType);
    }
}
