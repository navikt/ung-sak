package no.nav.foreldrepenger.behandling.steg.simulering;

import static java.util.Collections.singletonList;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.økonomi.simulering.SimulerOppdragAksjonspunktUtleder;
import no.nav.foreldrepenger.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.SimuleringResultatDto;
import no.nav.vedtak.exception.TekniskException;

@BehandlingStegRef(kode = "SIMOPP")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class SimulerOppdragSteg implements BehandlingSteg {

    private static final int ÅPNINGSTID = 7;
    private static final int STENGETID = 21;

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private FptilbakeRestKlient fptilbakeRestKlient;

    SimulerOppdragSteg() {
        // for CDI proxy
    }

    @Inject
    public SimulerOppdragSteg(BehandlingRepositoryProvider repositoryProvider,
                              BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                              SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                              TilbakekrevingRepository tilbakekrevingRepository,
                              FptilbakeRestKlient fptilbakeRestKlient) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        try {
            startSimulering(behandling);
            return utledAksjonspunkt(behandling);
        } catch (TekniskException e) {
            opprettFortsettBehandlingTask(behandling);
            return BehandleStegResultat.settPåVent();
        }
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        startSimulering(behandling);
        return utledAksjonspunkt(behandling);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.SIMULER_OPPDRAG.equals(tilSteg)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            simuleringIntegrasjonTjeneste.kansellerSimulering(behandling);
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingValg(behandling);
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingInntrekk(behandling);
        }
    }

    private void startSimulering(Behandling behandling) {
        simuleringIntegrasjonTjeneste.startSimulering(behandling);
    }

    private void opprettFortsettBehandlingTask(Behandling behandling) {
        LocalDateTime nesteKjøringEtter = utledNesteKjøring();
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(behandling,
            BehandlingStegType.SIMULER_OPPDRAG, nesteKjøringEtter);
    }

    private BehandleStegResultat utledAksjonspunkt(Behandling behandling) {
        if (harÅpenTilbakekreving(behandling)) {
            lagreTilbakekrevingValg(behandling, TilbakekrevingValg.medOppdaterTilbakekrevingsbehandling());
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Optional<SimuleringResultatDto> simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling.getId());
        if (simuleringResultatDto.isPresent()) {
            tilbakekrevingRepository.lagre(behandling, simuleringResultatDto.get().isSlåttAvInntrekk());

            Optional<AksjonspunktDefinisjon> utledetAksjonspunkt = SimulerOppdragAksjonspunktUtleder.utledAksjonspunkt(simuleringResultatDto.get());
            if (utledetAksjonspunkt.isPresent()) {
                AksjonspunktDefinisjon aksjonspunktDefinisjon = utledetAksjonspunkt.get();
                //(Team Tonic) Midlertidig løsning for automatisk inntrekk inntil vi har funksjonalitet for å slå det av
                if (aksjonspunktDefinisjon.equals(AksjonspunktDefinisjon.VURDER_INNTREKK)) {
                    lagreTilbakekrevingValg(behandling, TilbakekrevingValg.medAutomatiskInntrekk());
                    return BehandleStegResultat.utførtUtenAksjonspunkter();
                }
                return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(aksjonspunktDefinisjon));
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDateTime utledNesteKjøring() {
        LocalDateTime currentTime = LocalDateTime.now();
        if (DayOfWeek.SATURDAY.equals(currentTime.getDayOfWeek()) || DayOfWeek.SUNDAY.equals(currentTime.getDayOfWeek())) {
            return kommendeMandag(currentTime);
        } else if (DayOfWeek.FRIDAY.equals(currentTime.getDayOfWeek()) && currentTime.getHour() > STENGETID) {
            return kommendeMandag(currentTime);
        } else if (currentTime.getHour() < ÅPNINGSTID) {
            return currentTime.withHour(ÅPNINGSTID).withMinute(15);
        } else if (currentTime.getHour() > STENGETID) {
            return currentTime.plusDays(1).withHour(ÅPNINGSTID).withMinute(15);
        }
        return null; // bruker default innenfor åpningstid
    }

    private LocalDateTime kommendeMandag(LocalDateTime currentTime) {
        return currentTime.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(ÅPNINGSTID).withMinute(15);
    }

    private boolean harÅpenTilbakekreving(Behandling behandling) {
        return fptilbakeRestKlient.harÅpenTilbakekrevingsbehandling(behandling.getFagsak().getSaksnummer());
    }

    private void lagreTilbakekrevingValg(Behandling behandling, TilbakekrevingValg tilbakekrevingValg) {
        tilbakekrevingRepository.lagre(behandling, tilbakekrevingValg);
    }
}
