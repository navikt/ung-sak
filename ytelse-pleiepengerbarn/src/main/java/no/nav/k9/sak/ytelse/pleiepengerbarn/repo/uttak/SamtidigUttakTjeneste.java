package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Endringsstatus;
import no.nav.pleiepengerbarn.uttak.kontrakter.Simulering;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;

@Dependent
public class SamtidigUttakTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SamtidigUttakTjeneste.class);
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UttakTjeneste uttakTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private SamtidigUttakOverlappsjekker samtidigUttakOverlappsjekker;
    private boolean enableRelevantsjekk;


    @Inject
    public SamtidigUttakTjeneste(MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                 UttakTjeneste uttakTjeneste,
                                 FagsakRepository fagsakRepository,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingModellRepository behandlingModellRepository,
                                 SamtidigUttakOverlappsjekker samtidigUttakOverlappsjekker,
                                 @KonfigVerdi(value = "ENABLE_SAMTIDIG_UTTAK_RELEVANTSJEKK", defaultVerdi = "false") boolean enableRelevantsjekk) {
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingModellRepository = behandlingModellRepository;
        this.samtidigUttakOverlappsjekker = samtidigUttakOverlappsjekker;
        this.enableRelevantsjekk = enableRelevantsjekk;
    }


    public boolean isAnnenSakSomMåBehandlesFørst(BehandlingReferanse ref) {
        final List<Fagsak> andreFagsaker = hentAndreFagsakerPåPleietrengende(ref);
        final List<Behandling> andreÅpneBehandlinger = åpneBehandlingerFra(andreFagsaker);

        if (andreÅpneBehandlinger.isEmpty()) {
            return false;
        }

        if (enableRelevantsjekk && !samtidigUttakOverlappsjekker.isHarRelevantOverlappMedAndreUbehandledeSaker(ref)) {
            return false;
        }

        if (anyHarÅpenBehandlingSomIkkeHarKommetTilUttak(andreÅpneBehandlinger)) {
            /*
             * Krever at andre behandlinger, med relevant overlapp, kommer til uttak før vi går videre.
             */
            return true;
        }

        if (!isEndringerMedUbesluttedeData(ref)) {
            return false;
        }

        if (anyHarÅpenBehandlingSomKanBesluttes(andreÅpneBehandlinger)) {
            return true;
        }

        return false;
    }

    private List<Fagsak> hentAndreFagsakerPåPleietrengende(BehandlingReferanse ref) {
        final List<Fagsak> fagsaker = fagsakRepository.finnFagsakRelatertTil(ref.getFagsakYtelseType(), ref.getPleietrengendeAktørId(), null, null, null);
        //TODO PLS, riktig semantikk (!any vs none)?
        if (fagsaker.stream().noneMatch(f -> f.getSaksnummer().equals(ref.getSaksnummer()))) {
            throw new IllegalStateException("Utviklerfeil: Klarte ikke å finne saken.");
        }
        return fagsaker.stream().filter(f -> !f.getSaksnummer().equals(ref.getSaksnummer())).toList();
    }

    private List<Behandling> åpneBehandlingerFra(final List<Fagsak> fagsaker) {
        return fagsaker.stream()
            .map(f -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(f.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(b -> !b.getStatus().erFerdigbehandletStatus())
            .toList();

    }

    private boolean anyHarÅpenBehandlingSomIkkeHarKommetTilUttak(List<Behandling> åpneBehandlinger) {
        return åpneBehandlinger.stream().anyMatch(behandling -> {
            final BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
            return !harKommetTilUttak(ref);
        });
    }

    private boolean anyHarÅpenBehandlingSomKanBesluttes(List<Behandling> åpneBehandlinger) {
        return åpneBehandlinger.stream().anyMatch(behandling -> {
            final BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
            return !isEndringerMedUbesluttedeData(ref);
        });
    }

    public boolean isSkalHaTilbakehopp(BehandlingReferanse ref) {
        if (!harKommetTilUttak(ref)) {
            return false;
        }

        final Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final boolean harÅpentVenteaksjonspunkt = behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK).isPresent();

        final boolean annenSakSomMåBehandlesFørst = isAnnenSakSomMåBehandlesFørst(ref);
        if (annenSakSomMåBehandlesFørst && !harÅpentVenteaksjonspunkt) {
            // Send til steg for å sette på vent.
            return true;
        }

        if (!annenSakSomMåBehandlesFørst && harÅpentVenteaksjonspunkt) {
            // Fremtving at steget skal kjøres på nytt for å fjerne venting.
            return true;
        }

        // Det skal kun slippes én behandling gjennom.

        final Simulering simulering = simulerUttakKunBesluttet(ref);

        return simulering.getUttakplanEndret();
    }

    public boolean harKommetTilUttak(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return !modell.erStegAFørStegB(steg, BehandlingStegType.VURDER_UTTAK_V2);
    }

    private Simulering simulerUttak(BehandlingReferanse ref) {
        final Uttaksgrunnlag uttaksgrunnlag = mapInputTilUttakTjeneste.hentUtUbesluttededataOgMapRequest(ref);

        return uttakTjeneste.simulerUttaksplan(uttaksgrunnlag);
    }

    private Simulering simulerUttakKunBesluttet(BehandlingReferanse ref) {
        final Uttaksgrunnlag uttaksGrunnlag = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref);

        return uttakTjeneste.simulerUttaksplan(uttaksGrunnlag);
    }

    public boolean isEndringerMedUbesluttedeData(BehandlingReferanse ref) {
        final Simulering simulering = simulerUttak(ref);
        // Hvis en sak ikke har kommet til uttak betyr det at true returneres her.
        return simulering.getUttakplanEndret();
    }

    public NavigableSet<DatoIntervallEntitet> perioderMedEndringerMedUbesluttedeData(BehandlingReferanse ref) {
        final Simulering simulering = simulerUttak(ref);
        // Hvis en sak ikke har kommet til uttak betyr det at true returneres her.
        if (simulering.getUttakplanEndret()) {
            return simulering.getSimulertUttaksplan().getPerioder().entrySet().stream()
                .filter(entry -> entry.getValue().getEndringsstatus() == Endringsstatus.ENDRET)
                .map(entry -> DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey().getFom(), entry.getKey().getTom()))
                .collect(Collectors.toCollection(TreeSet::new));
        }
        return new TreeSet<>();
    }
}
