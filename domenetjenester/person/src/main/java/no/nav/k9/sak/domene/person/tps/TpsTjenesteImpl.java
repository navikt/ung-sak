package no.nav.k9.sak.domene.person.tps;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.xml.ws.soap.SOAPFaultException;

import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

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
    public Optional<Personinfo> hentBrukerForFnr(PersonIdent fnr) {
        if (fnr.erFdatNummer()) {
            return Optional.empty();
        }
        Optional<AktørId> aktørId = personinfoAdapter.hentAktørIdForPersonIdent(fnr);
        if (aktørId.isEmpty()) {
            return Optional.empty();
        }
        try {
            Personinfo personinfo = personinfoAdapter.hentKjerneinformasjon(aktørId.get());
            return Optional.ofNullable(personinfo);
        } catch (SOAPFaultException e) {
            if (e.getMessage().contains("status: S100008F")) {
                // Her sorterer vi ut dødfødte barn
                return Optional.empty();
            } else {
                throw e;
            }
        }
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
    public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
        return personinfoAdapter.hentAktørIdForPersonIdent(fnr);
    }

    @Override
    public Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return personinfoAdapter.hentIdentForAktørId(aktørId);
    }

    @Override
    public Optional<Personinfo> hentBrukerForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        return funnetFnr.map(fnr -> personinfoAdapter.hentKjerneinformasjon(aktørId));
    }

    @Override
    public Optional<String> hentDiskresjonskodeForAktør(PersonIdent fnr) {
        if (fnr.erFdatNummer()) {
            return Optional.empty();
        }
        return Optional.ofNullable(hentGeografiskTilknytning(fnr).getDiskresjonskode());
    }

    @Override
    public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent fnr) {
        return personinfoAdapter.hentGeografiskTilknytning(fnr);
    }

    @Override
    public List<GeografiskTilknytning> hentDiskresjonskoderForFamilierelasjoner(PersonIdent fnr) {
        return personinfoAdapter.hentDiskresjonskoderForFamilierelasjoner(fnr);
    }

    @Override
    public Adresseinfo hentAdresseinformasjon(PersonIdent personIdent) {
        return personinfoAdapter.hentAdresseinformasjon(personIdent);
    }

}
