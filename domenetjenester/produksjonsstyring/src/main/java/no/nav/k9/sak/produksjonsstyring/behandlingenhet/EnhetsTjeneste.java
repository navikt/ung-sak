package no.nav.k9.sak.produksjonsstyring.behandlingenhet;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.produksjonsstyring.arbeidsfordeling.ArbeidsfordelingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

@ApplicationScoped
public class EnhetsTjeneste {

    static class EnhetsTjenesteData {
        OrganisasjonsEnhet enhetKode6;
        OrganisasjonsEnhet enhetKlage;
        List<OrganisasjonsEnhet> alleBehandlendeEnheter;
        LocalDate sisteInnhenting = LocalDate.MIN;
    }

    private TpsTjeneste tpsTjeneste;
    private ArbeidsfordelingTjeneste arbeidsfordelingTjeneste;

    private Map<FagsakYtelseType, EnhetsTjenesteData> cache = Map.of(
        FagsakYtelseType.OMSORGSPENGER, new EnhetsTjenesteData(),
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN, new EnhetsTjenesteData(),
        FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, new EnhetsTjenesteData(),
        FagsakYtelseType.OPPLÆRINGSPENGER, new EnhetsTjenesteData(),
        FagsakYtelseType.FRISINN, new EnhetsTjenesteData());

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(TpsTjeneste tpsTjeneste, ArbeidsfordelingTjeneste arbeidsfordelingTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
        this.arbeidsfordelingTjeneste = arbeidsfordelingTjeneste;
    }

    List<OrganisasjonsEnhet> hentEnhetListe(FagsakYtelseType ytelseType) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        return cacheEntry.alleBehandlendeEnheter;
    }

    OrganisasjonsEnhet hentEnhetSjekkRegistrerteRelasjoner(AktørId aktørId, FagsakYtelseType ytelseType) {
        oppdaterEnhetCache(ytelseType);
        PersonIdent fnr = tpsTjeneste.hentFnrForAktør(aktørId);

        GeografiskTilknytning geografiskTilknytning = tpsTjeneste.hentGeografiskTilknytning(fnr);
        String aktivDiskresjonskode = geografiskTilknytning.getDiskresjonskode();
        if (!Diskresjonskode.KODE6.getKode().equals(aktivDiskresjonskode)) {
            boolean relasjonMedK6 = tpsTjeneste.hentDiskresjonskoderForFamilierelasjoner(fnr).stream()
                .anyMatch(geo -> Diskresjonskode.KODE6.getKode().equals(geo.getDiskresjonskode()));
            if (relasjonMedK6) {
                aktivDiskresjonskode = Diskresjonskode.KODE6.getKode();
            }
        }

        return arbeidsfordelingTjeneste.finnBehandlendeEnhet(geografiskTilknytning.getTilknytning(), aktivDiskresjonskode, ytelseType);
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgitte(FagsakYtelseType ytelseType, String enhetId, List<AktørId> relaterteAktører) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        if (cacheEntry.enhetKode6.getEnhetId().equals(enhetId) || cacheEntry.enhetKlage.getEnhetId().equals(enhetId)) {
            return Optional.empty();
        }

        return sjekkSpesifiserteRelaterte(cacheEntry, relaterteAktører);
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkRegistrerteRelasjoner(FagsakYtelseType ytelseType, String enhetId, AktørId aktørId, Optional<AktørId> kobletAktørId,
                                                                         List<AktørId> relaterteAktører) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        if (cacheEntry.enhetKode6.getEnhetId().equals(enhetId) || cacheEntry.enhetKlage.getEnhetId().equals(enhetId)) {
            return Optional.empty();
        }

        OrganisasjonsEnhet enhet = hentEnhetSjekkRegistrerteRelasjoner(aktørId, ytelseType);
        if (cacheEntry.enhetKode6.getEnhetId().equals(enhet.getEnhetId())) {
            return Optional.of(cacheEntry.enhetKode6);
        }
        if (kobletAktørId.isPresent()) {
            OrganisasjonsEnhet enhetKoblet = hentEnhetSjekkRegistrerteRelasjoner(kobletAktørId.get(), ytelseType);
            if (cacheEntry.enhetKode6.getEnhetId().equals(enhetKoblet.getEnhetId())) {
                return Optional.of(cacheEntry.enhetKode6);
            }
        }
        if (sjekkSpesifiserteRelaterte(cacheEntry, relaterteAktører).isPresent()) {
            return Optional.of(cacheEntry.enhetKode6);
        }
        if (!gyldigEnhetId(ytelseType, enhetId)) {
            return Optional.of(enhet);
        }

        return Optional.empty();
    }

    private Optional<OrganisasjonsEnhet> sjekkSpesifiserteRelaterte(EnhetsTjenesteData cacheEntry, List<AktørId> relaterteAktører) {
        for (AktørId relatert : relaterteAktører) {
            PersonIdent personIdent = tpsTjeneste.hentFnrForAktør(relatert);
            GeografiskTilknytning geo = tpsTjeneste.hentGeografiskTilknytning(personIdent);
            if (Diskresjonskode.KODE6.getKode().equals(geo.getDiskresjonskode())) {
                return Optional.of(cacheEntry.enhetKode6);
            }
        }
        return Optional.empty();
    }

    private EnhetsTjenesteData oppdaterEnhetCache(FagsakYtelseType ytelseType) {
        var cacheEntry = cache.get(ytelseType);
        if (cacheEntry.sisteInnhenting.isBefore(LocalDate.now())) {
            synchronized (cacheEntry) {
                if (cacheEntry.sisteInnhenting.isBefore(LocalDate.now())) {
                    cacheEntry.enhetKode6 = arbeidsfordelingTjeneste.hentEnhetForDiskresjonskode(Diskresjonskode.KODE6.getKode(), ytelseType);
                    cacheEntry.enhetKlage = arbeidsfordelingTjeneste.getKlageInstansEnhet();
                    cacheEntry.alleBehandlendeEnheter = arbeidsfordelingTjeneste.finnAlleBehandlendeEnhetListe(ytelseType);
                    cacheEntry.sisteInnhenting = LocalDate.now();
                }
            }
        }
        return cacheEntry;
    }

    private boolean gyldigEnhetId(FagsakYtelseType ytelseType, String enhetId) {
        return finnOrganisasjonsEnhet(ytelseType, enhetId).isPresent();
    }

    Optional<OrganisasjonsEnhet> finnOrganisasjonsEnhet(FagsakYtelseType ytelseType, String enhetId) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        return cacheEntry.alleBehandlendeEnheter.stream().filter(e -> enhetId.equals(e.getEnhetId())).findFirst();
    }

}
