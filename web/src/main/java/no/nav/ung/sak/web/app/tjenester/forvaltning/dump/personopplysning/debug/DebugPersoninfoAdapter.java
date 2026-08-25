package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.personopplysning.debug;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DebugPersoninfoAdapter {
    private DebugPersoninfoTjeneste debugPersoninfoTjeneste;
    private AktørTjeneste aktørTjeneste;

    public DebugPersoninfoAdapter() {
        // for CDI proxy
    }

    @Inject
    public DebugPersoninfoAdapter(DebugPersoninfoTjeneste debugPersoninfoTjeneste, AktørTjeneste aktørTjeneste) {
        this.debugPersoninfoTjeneste = debugPersoninfoTjeneste;
        this.aktørTjeneste = aktørTjeneste;
    }

    public Personinfo hentPersoninfo(List<String> dumpinnhold, AktørId aktørId, FagsakYtelseType ytelseType) {
        return hentKjerneinformasjon(dumpinnhold, aktørId, ytelseType);
    }


    /**
     * Henter PersonInfo for barn, gitt at det ikke er FDAT nummer (sjekkes på format av PersonIdent, evt. ved feilhåndtering fra TPS). Hvis
     * FDAT nummer returneres {@link Optional#empty()}
     */
    public Optional<Personinfo> innhentSaksopplysningerForBarn(PersonIdent personIdent, List<String> dumpinnhold, FagsakYtelseType ytelseType) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        Optional<AktørId> optAktørId = hentAktørIdForPersonIdent(personIdent);
        if (optAktørId.isPresent()) {
            return hentKjerneinformasjonFor(dumpinnhold, optAktørId.get(), personIdent, ytelseType);
        }
        return Optional.empty();
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

    private Optional<Personinfo> hentKjerneinformasjonFor(List<String> dumpinnhold, AktørId aktørId, PersonIdent personIdent, FagsakYtelseType ytelseType) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        return Optional.of(hentKjerneinformasjon(dumpinnhold, aktørId, personIdent, ytelseType));
    }

    public Personinfo hentKjerneinformasjon(List<String> dumpinnhold, AktørId aktørId, FagsakYtelseType ytelseType) {
        var personIdent = hentFnr(aktørId);
        return hentKjerneinformasjon(dumpinnhold, aktørId, personIdent, ytelseType);
    }

    private Personinfo hentKjerneinformasjon(List<String> dumpinnhold, AktørId aktørId, PersonIdent personIdent, FagsakYtelseType ytelseType) {
        return debugPersoninfoTjeneste.hentKjerneinformasjon(dumpinnhold, aktørId, personIdent, ytelseType);
    }

    private PersonIdent hentFnr(AktørId aktørId) {
        return hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Finner ikke FNR for angitt aktørId"));
    }

}
