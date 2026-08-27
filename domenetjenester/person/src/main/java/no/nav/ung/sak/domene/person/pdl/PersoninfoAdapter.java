package no.nav.ung.sak.domene.person.pdl;

import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@ApplicationScoped
public class PersoninfoAdapter {

    private static final Logger log = LoggerFactory.getLogger(PersoninfoAdapter.class);
    private PersonBasisTjeneste personBasisTjeneste;
    private PersoninfoTjeneste personinfoTjeneste;
    private AktørTjeneste aktørTjeneste;
    private TilknytningTjeneste tilknytningTjeneste;

    private FagsakRepository fagsakRepository;

    public PersoninfoAdapter() {
        // for CDI proxy
    }

    @Inject
    public PersoninfoAdapter(PersonBasisTjeneste personBasisTjeneste, PersoninfoTjeneste personinfoTjeneste, AktørTjeneste aktørTjeneste, TilknytningTjeneste tilknytningTjeneste, FagsakRepository fagsakRepository) {
        this.personBasisTjeneste = personBasisTjeneste;
        this.personinfoTjeneste = personinfoTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.tilknytningTjeneste = tilknytningTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    public Personinfo hentPersoninfo(AktørId aktørId, FagsakYtelseType ytelseType) {
        return hentKjerneinformasjon(aktørId, ytelseType);
    }

    public Optional<Personinfo> innhentSaksopplysninger(PersonIdent personIdent, FagsakYtelseType ytelseType) {
        Optional<AktørId> aktørId = hentAktørIdForPersonIdent(personIdent);

        if (aktørId.isPresent()) {
            return hentKjerneinformasjonFor(aktørId.get(), personIdent, ytelseType);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Henter PersonInfo for barn, gitt at det ikke er FDAT nummer (sjekkes på format av PersonIdent, evt. ved feilhåndtering fra TPS). Hvis
     * FDAT nummer returneres {@link Optional#empty()}
     */
    public Optional<Personinfo> innhentSaksopplysningerForBarn(PersonIdent personIdent, FagsakYtelseType ytelseType) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        Optional<AktørId> optAktørId = hentAktørIdForPersonIdent(personIdent);
        if (optAktørId.isPresent()) {
            return hentKjerneinformasjonFor(optAktørId.get(), personIdent, ytelseType);
        }
        return Optional.empty();
    }

    public Optional<PersoninfoArbeidsgiver> hentPersoninfoArbeidsgiver(AktørId aktørId, FagsakYtelseType ytelseType) {
        var pi = hentFnr(aktørId);
        return Optional.ofNullable(personBasisTjeneste.hentPersoninfoArbeidsgiver(aktørId, pi, ytelseType));
    }

    public Optional<PersoninfoBasis> hentBrukerBasisForAktør(AktørId aktørId,FagsakYtelseType ytelseType) {
        var personIdent = hentFnr(aktørId);
        return Optional.ofNullable(personBasisTjeneste.hentBasisPersoninfo(aktørId, personIdent, ytelseType));
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

    private Optional<Personinfo> hentKjerneinformasjonFor(AktørId aktørId, PersonIdent personIdent, FagsakYtelseType ytelseType) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        return Optional.of(hentKjerneinformasjon(aktørId, personIdent, ytelseType));
    }

    public Personinfo hentKjerneinformasjon(AktørId aktørId, FagsakYtelseType ytelseType) {
        var personIdent = hentFnr(aktørId);
        return hentKjerneinformasjon(aktørId, personIdent, ytelseType);
    }

    public Personinfo hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent, FagsakYtelseType ytelseType) {
        return personinfoTjeneste.hentKjerneinformasjon(aktørId, personIdent, ytelseType);
    }

    private PersonIdent hentFnr(AktørId aktørId) {
        var personIdent = hentIdentForAktørId(aktørId);
        if (personIdent.isEmpty()) {
            var fagsaker = fagsakRepository.hentForBruker(aktørId);
            var saksnummer = fagsaker.stream().map(Fagsak::getSaksnummer).collect(Collectors.toSet());
            log.warn("Fant ikke FNR for bruker med saksnummer " + saksnummer);
        }
        return personIdent.orElseThrow(() -> new IllegalStateException("Finner ikke FNR for angitt aktørId"));
    }

    public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent personIdent, FagsakYtelseType ytelseType) {
        var aktørId = hentAktørIdForPersonIdent(personIdent).orElseThrow(() -> new IllegalStateException("Kan ikke finne geografisk tilknytning for fnr med ukjent aktørId"));
        return tilknytningTjeneste.hentGeografiskTilknytning(aktørId, ytelseType);
    }

}
