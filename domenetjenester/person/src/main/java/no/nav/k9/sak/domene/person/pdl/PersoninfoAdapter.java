package no.nav.k9.sak.domene.person.pdl;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;

@ApplicationScoped
public class PersoninfoAdapter {
    private PersonBasisTjeneste personBasisTjeneste;
    private PersoninfoTjeneste personinfoTjeneste;
    private AktørTjeneste aktørTjeneste;
    private TilknytningTjeneste tilknytningTjeneste;

    public PersoninfoAdapter() {
        // for CDI proxy
    }

    @Inject
    public PersoninfoAdapter(PersonBasisTjeneste personBasisTjeneste, PersoninfoTjeneste personinfoTjeneste, AktørTjeneste aktørTjeneste, TilknytningTjeneste tilknytningTjeneste) {
        this.personBasisTjeneste = personBasisTjeneste;
        this.personinfoTjeneste = personinfoTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.tilknytningTjeneste = tilknytningTjeneste;
    }

    public Personinfo hentPersoninfo(AktørId aktørId) {
        return hentKjerneinformasjon(aktørId);
    }

    public Optional<Personinfo> innhentSaksopplysninger(PersonIdent personIdent) {
        Optional<AktørId> aktørId = hentAktørIdForPersonIdent(personIdent);

        if (aktørId.isPresent()) {
            return hentKjerneinformasjonFor(aktørId.get(), personIdent);
        } else {
            return Optional.empty();
        }
    }

    public Personhistorikkinfo innhentPersonopplysningerHistorikk(AktørId aktørId, Periode periode) {
        return personinfoTjeneste.hentPersoninfoHistorikk(aktørId, periode);
    }

    /**
     * Henter PersonInfo for barn, gitt at det ikke er FDAT nummer (sjekkes på format av PersonIdent, evt. ved feilhåndtering fra TPS). Hvis
     * FDAT nummer returneres {@link Optional#empty()}
     */
    public Optional<Personinfo> innhentSaksopplysningerForBarn(PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        Optional<AktørId> optAktørId = hentAktørIdForPersonIdent(personIdent);
        if (optAktørId.isPresent()) {
            return hentKjerneinformasjonFor(optAktørId.get(), personIdent);
        }
        return Optional.empty();
    }

    public Optional<PersoninfoArbeidsgiver> hentPersoninfoArbeidsgiver(AktørId aktørId) {
        var pi = hentFnr(aktørId);
        return Optional.ofNullable(personBasisTjeneste.hentPersoninfoArbeidsgiver(aktørId, pi));
    }

    public Optional<PersoninfoBasis> hentBrukerBasisForAktør(AktørId aktørId) {
        var personIdent = hentFnr(aktørId);
        return Optional.ofNullable(personBasisTjeneste.hentBasisPersoninfo(aktørId, personIdent));
    }

    public Optional<PersonIdent> hentIdentForAktørId(AktørId aktørId) {
        return aktørTjeneste.hentPersonIdentForAktørId(aktørId);
    }

    public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        } else if (personIdent.erAktørId()) {
            return Optional.of(new AktørId(personIdent.getAktørId()));
        } else if (personIdent.erNorskIdent()) {
            return aktørTjeneste.hentAktørIdForPersonIdent(personIdent);
        } else {
            throw new IllegalArgumentException("Forventet norsk ident (fnr/dnr)");
        }
    }

    private Optional<Personinfo> hentKjerneinformasjonFor(AktørId aktørId, PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        return Optional.of(hentKjerneinformasjon(aktørId, personIdent));
    }

    public Personinfo hentKjerneinformasjon(AktørId aktørId) {
        var personIdent = hentFnr(aktørId);
        return hentKjerneinformasjon(aktørId, personIdent);
    }

    private Personinfo hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent) {
        return personinfoTjeneste.hentKjerneinformasjon(aktørId, personIdent);
    }

    private PersonIdent hentFnr(AktørId aktørId) {
        return hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Finner ikke FNR for angitt aktørId"));
    }

    public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent personIdent) {
        var aktørId = hentAktørIdForPersonIdent(personIdent).orElseThrow(() -> new IllegalStateException("Kan ikke finne geografisk tilknytning for fnr med ukjent aktørId"));
        return tilknytningTjeneste.hentGeografiskTilknytning(aktørId);
    }

}
