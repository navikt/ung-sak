package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.personopplysning.debug;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.k9.sak.domene.person.pdl.TilknytningTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;

@ApplicationScoped
public class DebugPersoninfoAdapter {
    private PersonBasisTjeneste personBasisTjeneste;
    private DebugPersoninfoTjeneste debugPersoninfoTjeneste;
    private AktørTjeneste aktørTjeneste;
    private TilknytningTjeneste tilknytningTjeneste;

    public DebugPersoninfoAdapter() {
        // for CDI proxy
    }

    @Inject
    public DebugPersoninfoAdapter(PersonBasisTjeneste personBasisTjeneste, DebugPersoninfoTjeneste debugPersoninfoTjeneste, AktørTjeneste aktørTjeneste, TilknytningTjeneste tilknytningTjeneste) {
        this.personBasisTjeneste = personBasisTjeneste;
        this.debugPersoninfoTjeneste = debugPersoninfoTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.tilknytningTjeneste = tilknytningTjeneste;
    }

    public Personinfo hentPersoninfo(List<String> dumpinnhold, AktørId aktørId) {
        return hentKjerneinformasjon(dumpinnhold, aktørId);
    }

    public Optional<Personinfo> innhentSaksopplysninger(PersonIdent personIdent, List<String> dumpinnhold) {
        Optional<AktørId> aktørId = hentAktørIdForPersonIdent(personIdent);

        if (aktørId.isPresent()) {
            return hentKjerneinformasjonFor(dumpinnhold, aktørId.get(), personIdent);
        } else {
            return Optional.empty();
        }
    }

    public Personhistorikkinfo innhentPersonopplysningerHistorikk(List<String> dumpinnhold, AktørId aktørId, Periode periode) {
        return debugPersoninfoTjeneste.hentPersoninfoHistorikk(dumpinnhold, aktørId, periode);
    }

    /**
     * Henter PersonInfo for barn, gitt at det ikke er FDAT nummer (sjekkes på format av PersonIdent, evt. ved feilhåndtering fra TPS). Hvis
     * FDAT nummer returneres {@link Optional#empty()}
     */
    public Optional<Personinfo> innhentSaksopplysningerForBarn(PersonIdent personIdent, List<String> dumpinnhold) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        Optional<AktørId> optAktørId = hentAktørIdForPersonIdent(personIdent);
        if (optAktørId.isPresent()) {
            return hentKjerneinformasjonFor(dumpinnhold, optAktørId.get(), personIdent);
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

    private Optional<Personinfo> hentKjerneinformasjonFor(List<String> dumpinnhold, AktørId aktørId, PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        return Optional.of(hentKjerneinformasjon(dumpinnhold, aktørId, personIdent));
    }

    public Personinfo hentKjerneinformasjon(List<String> dumpinnhold, AktørId aktørId) {
        var personIdent = hentFnr(aktørId);
        return hentKjerneinformasjon(dumpinnhold, aktørId, personIdent);
    }

    private Personinfo hentKjerneinformasjon(List<String> dumpinnhold, AktørId aktørId, PersonIdent personIdent) {
        return debugPersoninfoTjeneste.hentKjerneinformasjon(dumpinnhold, aktørId, personIdent);
    }

    private PersonIdent hentFnr(AktørId aktørId) {
        return hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Finner ikke FNR for angitt aktørId"));
    }

    public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent personIdent) {
        var aktørId = hentAktørIdForPersonIdent(personIdent).orElseThrow(() -> new IllegalStateException("Kan ikke finne geografisk tilknytning for fnr med ukjent aktørId"));
        return tilknytningTjeneste.hentGeografiskTilknytning(aktørId);
    }

}
