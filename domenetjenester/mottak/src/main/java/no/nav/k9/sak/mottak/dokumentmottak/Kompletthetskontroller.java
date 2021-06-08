package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandling.prosessering.ProsesseringsFeil;
import no.nav.k9.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.k9.sak.behandling.prosessering.task.GjenopptaBehandlingTask;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.k9.sak.kompletthet.KompletthetModell;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Denne klassen evaluerer hvilken effekt en ekstern hendelse (dokument, forretningshendelse) har på en åpen behandlings
 * kompletthet, og etterfølgende effekt på behandlingskontroll (gjennom {@link Endringskontroller})
 */
@Dependent
public class Kompletthetskontroller {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private KompletthetModell kompletthetModell;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public Kompletthetskontroller() {
        // For CDI proxy
    }

    @Inject
    public Kompletthetskontroller(DokumentmottakerFelles dokumentmottakerFelles,
                                  KompletthetModell kompletthetModell,
                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.kompletthetModell = kompletthetModell;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public ProsessTaskData asynkVurderKompletthet(Behandling behandling) {
        preconditionIkkeAksepterKobling(behandling);

        // Ta snapshot av gjeldende grunnlag-id-er før oppdateringer
        EndringsresultatSnapshot grunnlagSnapshot = behandlingProsesseringTjeneste.taSnapshotAvBehandlingsgrunnlag(behandling);

        // gjør komplethetsjekk i en senere task (etter at inntektsmeldinger er lagret til abakus)
        var kompletthetskontrollerTask = KompletthetskontrollerVurderKompletthetTask.init(behandling, grunnlagSnapshot);

        return kompletthetskontrollerTask;
    }

    public void vurderKompletthetOgFortsett(Behandling behandling, Long behandlingId, EndringsresultatSnapshot grunnlagSnapshot) {
        preconditionIkkeAksepterKobling(behandling);

        // Vurder kompletthet etter at dokument knyttet til behandling
        spolKomplettBehandlingTilStartpunkt(behandling, grunnlagSnapshot);
        if (kompletthetModell.erKompletthetssjekkPassert(behandlingId)) {
            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }
    }

    private void preconditionIkkeAksepterKobling(Behandling behandling) {
        if (behandling.getType() == BehandlingType.UNNTAKSBEHANDLING) {
            throw new UnsupportedOperationException("Kan ikke oppdatere åpen unntaksbehandling");
        }
        if (kompletthetModell.erKompletthetssjekkPassert(behandling.getId()) && !kompletthetModell.erRegisterInnhentingPassert(behandling.getId())) {
            throw ProsesseringsFeil.FACTORY.kanIkkePlanleggeNyTaskPgaVentendeTaskerPåBehandling(behandling.getId()).toException();
        } else {
            Set<String> treeAmigos = Set.of(StartBehandlingTask.TASKTYPE, FortsettBehandlingTask.TASKTYPE, GjenopptaBehandlingTask.TASKTYPE);
            behandlingProsesseringTjeneste.feilPågåendeTaskHvisFremtidigTaskEksisterer(behandling, treeAmigos);
        }
    }

    public void vurderKompletthetForKøetBehandling(Behandling behandling) {
        List<AksjonspunktDefinisjon> autoPunkter = kompletthetModell.rangerKompletthetsfunksjonerKnyttetTilAutopunkt(behandling.getFagsakYtelseType(),
            behandling.getType());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        for (AksjonspunktDefinisjon autopunkt : autoPunkter) {
            KompletthetResultat kompletthetResultat = kompletthetModell.vurderKompletthet(ref, autopunkt);
            if (!kompletthetResultat.erOppfylt()) {
                // Et av kompletthetskriteriene er ikke oppfylt, og evt. brev er sendt ut. Logger historikk og avbryter
                if (!kompletthetResultat.erFristUtløpt()) {
                    dokumentmottakerFelles.opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling,
                        HistorikkinnslagType.BEH_VENT, kompletthetResultat.getVentefrist(), kompletthetResultat.getVenteårsak());
                }
                return;
            }
        }
    }

    private void spolKomplettBehandlingTilStartpunkt(Behandling behandling, EndringsresultatSnapshot grunnlagSnapshot) {
        // Behandling er komplett - nullstill venting
        if (behandling.isBehandlingPåVent()) {
            behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        }
        if (kompletthetModell.erKompletthetssjekkPassert(behandling.getId())) {
            // Reposisjoner basert på grunnlagsendring i nylig mottatt dokument. Videre reposisjonering gjøres i task etter registeroppdatering
            EndringsresultatDiff diff = behandlingProsesseringTjeneste.finnGrunnlagsEndring(behandling, grunnlagSnapshot);
            behandlingProsesseringTjeneste.reposisjonerBehandlingVedEndringer(behandling, diff);
        }
    }

}
