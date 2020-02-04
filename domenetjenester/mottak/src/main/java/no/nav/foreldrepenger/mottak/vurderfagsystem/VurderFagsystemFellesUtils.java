package no.nav.foreldrepenger.mottak.vurderfagsystem;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingTema;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;

@ApplicationScoped
public class VurderFagsystemFellesUtils {

    private static final Period MAKS_AVVIK_DAGER_IM_INPUT = Period.parse("P9W");

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public VurderFagsystemFellesUtils(){
        //Injected normal scoped bean is now proxyable
    }

    @Inject
    public VurderFagsystemFellesUtils(BehandlingRepository behandlingRepository, 
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                      SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public List<Fagsak> filtrerSakerForBehandlingTema(List<Fagsak> saker, BehandlingTema behandlingTema) {
        if (BehandlingTema.ikkeSpesifikkHendelse(behandlingTema)) {
            return saker;
        }
        return saker.stream()
            .filter(s -> behandlingTema.erKompatibelMed(this.getBehandlingsTemaForFagsak(s)))
            .collect(Collectors.toList());
    }

    public List<Fagsak> finnÅpneSaker(List<Fagsak> saker) {
        return saker.stream()
            .filter(Fagsak::erÅpen)
            .filter(s -> FagsakStatus.LØPENDE.equals(s.getStatus()) || harÅpenYtelsesBehandling(s))
            .collect(Collectors.toList());
    }

    private boolean harÅpenYtelsesBehandling(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .anyMatch(b -> BehandlingType.FØRSTEGANGSSØKNAD.equals(b.getType()) || BehandlingType.REVURDERING.equals(b.getType()));
    }

    private List<Fagsak> harSakMedAvslagGrunnetManglendeDok(List<Fagsak> saker) {
        return saker.stream()
            .filter(s -> mottatteDokumentTjeneste.erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(s) && !mottatteDokumentTjeneste.harFristForInnsendingAvDokGåttUt(s))
            .collect(Collectors.toList());
    }

    public boolean erFagsakPassendeForFamilieHendelse(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        // Vurder omskriving av denne og neste til Predicate<Fagsak> basert på bruksmønster
        // Finn behandling
        Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandling.isEmpty()) {
            return true;
        }

        BehandlingTema bhTemaFagsak = Fagsak.fraFagsakHendelse(fagsak.getYtelseType());
        if (!vurderFagsystem.getBehandlingTema().erKompatibelMed(bhTemaFagsak)) {
            return false;
        }

        return false;
    }

    public boolean erFagsakPassendeForStartdato(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        // Finn behandling
        Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandling.isEmpty()) {
            return true;
        }

        if (vurderFagsystem.getStartDatoInntektsmelding().isPresent()) {
            LocalDate førsteDagBehandling = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.get().getId()).getFørsteUttaksdato();
            LocalDate startDato = vurderFagsystem.getStartDatoInntektsmelding().get();
            return førsteDagBehandling.minus(MAKS_AVVIK_DAGER_IM_INPUT).isBefore(startDato) && førsteDagBehandling.plus(MAKS_AVVIK_DAGER_IM_INPUT).isAfter(startDato);
        }
        return true;
    }

    public Optional<BehandlendeFagsystem> standardUstrukturertDokumentVurdering(List<Fagsak> sakerTilVurdering) {
        List<Fagsak> åpneFagsaker = finnÅpneSaker(sakerTilVurdering);
        if (åpneFagsaker.size() == 1) {
            return Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(åpneFagsaker.get(0).getSaksnummer()));
        } else if (åpneFagsaker.size() > 1) {
            return Optional.of(new BehandlendeFagsystem(MANUELL_VURDERING));
        }
        List<Fagsak> avslagDokumentasjon = harSakMedAvslagGrunnetManglendeDok(sakerTilVurdering);
        if (avslagDokumentasjon.size() == 1) {
            return Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(avslagDokumentasjon.get(0).getSaksnummer()));
        }
        return Optional.empty();
    }

    public static boolean erSøknad(VurderFagsystem vurderFagsystem) {
        return (vurderFagsystem.getDokumentTypeId() != null && DokumentTypeId.getSøknadTyper().contains(vurderFagsystem.getDokumentTypeId().getKode())) ||
            (vurderFagsystem.getDokumentKategori() != null && DokumentKategori.SØKNAD.equals(vurderFagsystem.getDokumentKategori()));
    }

    private BehandlingTema getBehandlingsTemaForFagsak(Fagsak s) {
        Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(s.getId());
        // FIXME K9 kodeverk/logikk
        if (behandling.isEmpty()) {
            return Fagsak.fraFagsakHendelse(s.getYtelseType());
        }
        return Fagsak.fraFagsakHendelse(s.getYtelseType());
    }
}
