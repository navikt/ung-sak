package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;

/**
 * Ventepunkt for vedtaksprioritet, rett før {@link BehandlingStegType#BEREGN_YTELSE}. Bare én ytelsesbehandling per
 * fagsak har vedtaksprioritet og kan gå videre, slik at to åpne behandlinger aldri skriver tilkjent ytelse og sender til
 * oppdrag samtidig.
 * <p>
 * Behandlingen settes på vent når
 * <ul>
 * <li>den ikke bygger på siste vedtak. Da opprettes synkronisering av åpne behandlinger hvis den ikke finnes.</li>
 * <li>en annen åpen behandling på fagsaken har vedtaksprioritet.</li>
 * </ul>
 * Autopunktet hopper tilbake til ventepunktet ved gjenopptakelse, så sjekken kjøres på nytt uansett hvordan
 * behandlingen tas av vent.
 */
@BehandlingStegRef(BehandlingStegType.VURDER_VEDTAKSPRIORITET)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderVedtaksprioritetSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(VurderVedtaksprioritetSteg.class);
    private static final AksjonspunktDefinisjon VENTEPUNKT = AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_VEDTAKSPRIORITET;

    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private VedtaksprioritetTjeneste vedtaksprioritetTjeneste;
    private SynkroniserÅpneBehandlingerTaskOppretter synkroniseringTaskOppretter;

    VurderVedtaksprioritetSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderVedtaksprioritetSteg(BehandlingRepository behandlingRepository,
                                      FagsakLåsRepository fagsakLåsRepository,
                                      VedtaksprioritetTjeneste vedtaksprioritetTjeneste,
                                      SynkroniserÅpneBehandlingerTaskOppretter synkroniseringTaskOppretter) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakLåsRepository = fagsakLåsRepository;
        this.vedtaksprioritetTjeneste = vedtaksprioritetTjeneste;
        this.synkroniseringTaskOppretter = synkroniseringTaskOppretter;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long fagsakId = kontekst.getFagsakId();
        Long behandlingId = kontekst.getBehandlingId();

        // Tasken holder allerede låsen på egen behandling. Låsrekkefølgen er behandling før fagsak, så steget låser
        // aldri andre behandlinger. Fagsaklåsen holdes til transaksjonen er ferdig, slik at sjekken og stegovergangen
        // skjer samlet og to behandlinger ikke kan passere samtidig.
        fagsakLåsRepository.taLås(fagsakId);

        // Lesingene under går forbi Hibernate-sesjonen og ser det som er committet før låsen ble tatt.
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!vedtaksprioritetTjeneste.byggerPåSisteVedtak(fagsakId, behandlingId)) {
            log.info("Behandling {} i sak {} bygger ikke på siste vedtak, venter på synkronisering", behandlingId, behandling.getFagsak().getSaksnummer());
            synkroniseringTaskOppretter.opprettHvisIkkeFinnes(fagsakId);
            return settPåVent();
        }

        var behandlingMedVedtaksprioritet = vedtaksprioritetTjeneste.finnBehandlingMedVedtaksprioritet(behandling.getFagsakYtelseType(), fagsakId, behandlingId);
        if (behandlingMedVedtaksprioritet.isPresent()) {
            log.info("Behandling {} i sak {} venter på behandling {}, som har vedtaksprioritet", behandlingId, behandling.getFagsak().getSaksnummer(), behandlingMedVedtaksprioritet.get().behandlingId());
            return settPåVent();
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static BehandleStegResultat settPåVent() {
        var frist = LocalDateTime.now().plus(VENTEPUNKT.getFristPeriod());
        return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(
            AksjonspunktResultat.opprettForAksjonspunktMedFrist(VENTEPUNKT, Venteårsak.VENT_TIDLIGERE_BEHANDLING, frist)));
    }
}
