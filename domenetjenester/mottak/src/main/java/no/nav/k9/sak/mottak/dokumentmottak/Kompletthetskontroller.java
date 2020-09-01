package no.nav.k9.sak.mottak.dokumentmottak;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_KOMPLETT_OPPDATERING;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandling.prosessering.ProsesseringsFeil;
import no.nav.k9.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.k9.sak.behandling.prosessering.task.GjenopptaBehandlingTask;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.k9.sak.kompletthet.KompletthetModell;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Denne klassen evaluerer hvilken effekt en ekstern hendelse (dokument, forretningshendelse) har på en åpen behandlings
 * kompletthet, og etterfølgende effekt på behandlingskontroll (gjennom {@link Endringskontroller})
 */
@Dependent
public class Kompletthetskontroller {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private KompletthetModell kompletthetModell;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public Kompletthetskontroller() {
        // For CDI proxy
    }

    @Inject
    public Kompletthetskontroller(DokumentmottakerFelles dokumentmottakerFelles,
                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                  KompletthetModell kompletthetModell,
                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.kompletthetModell = kompletthetModell;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void persisterDokumentOgVurderKompletthet(Behandling behandling, Collection<MottattDokument> mottattDokument) {
        // Ta snapshot av gjeldende grunnlag-id-er før oppdateringer
        Long behandlingId = behandling.getId();

        preconditionIkkeAksepterKobling(behandling);

        // ta snapshot før lagre inntektsmelding
        EndringsresultatSnapshot grunnlagSnapshot = behandlingProsesseringTjeneste.taSnapshotAvBehandlingsgrunnlag(behandling);

        // Persister dokument (dvs. knytt dokument til behandlingen)
        mottatteDokumentTjeneste.persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(behandling, mottattDokument);

        // Vurder kompletthet etter at dokument knyttet til behandling
        var kompletthetResultat = vurderBehandlingKomplett(behandling);
        if (!kompletthetResultat.erOppfylt()) {
            settPåVent(behandling, kompletthetResultat);
        } else {
            spolKomplettBehandlingTilStartpunkt(behandling, grunnlagSnapshot);
            if (kompletthetModell.erKompletthetssjekkPassert(behandlingId)) {
                behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false);
            } else {
                behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
            }
        }

    }

    private void preconditionIkkeAksepterKobling(Behandling behandling) {
        Long behandlingId = behandling.getId();
        if (kompletthetModell.erKompletthetssjekkPassert(behandlingId) && !kompletthetModell.erRegisterInnhentingPassert(behandlingId)) {
            throw ProsesseringsFeil.FACTORY.kanIkkePlanleggeNyTaskPgaVentendeTaskerPåBehandling(behandlingId).toException();
        } else {
            Set<String> treeAmigos = Set.of(StartBehandlingTask.TASKTYPE, FortsettBehandlingTask.TASKTYPE, GjenopptaBehandlingTask.TASKTYPE);
            behandlingProsesseringTjeneste.feilPågåendeTaskHvisFremtidigTaskEksisterer(behandling, treeAmigos);
        }
    }

    private void settPåVent(Behandling behandling, KompletthetResultat kompletthetResultat) {
        if (kompletthetResultat.erFristUtløpt() || behandling.isBehandlingPåVent()) {
            // Tidsfrist for kompletthetssjekk er utløpt, skal derfor ikke settes på vent på nytt
            return;
        }
        // TODO (JOL): Logikken nå reflekterer det som lå i EndrKontroll. Avklar om andre autopunkt skal erstattes med det under.
        // Settes på vent til behandlig er komplett
        behandlingProsesseringTjeneste.settBehandlingPåVent(behandling, AUTO_VENT_KOMPLETT_OPPDATERING,
            kompletthetResultat.getVentefrist(), kompletthetResultat.getVenteårsak(), kompletthetResultat.getVenteårsakVariant());
        dokumentmottakerFelles.opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling,
            HistorikkinnslagType.BEH_VENT, kompletthetResultat.getVentefrist(), kompletthetResultat.getVenteårsak());
    }

    /** for test only */
    void persisterKøetDokumentOgVurderKompletthet(Behandling behandling, List<MottattDokument> mottattDokument) {
        // Persister dokument (dvs. knytt dokument til behandlingen)
        mottatteDokumentTjeneste.persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(behandling, mottattDokument);
        vurderKompletthetForKøetBehandling(behandling);
    }

    public void oppdaterKompletthetForKøetBehandling(Behandling behandling) {
        vurderKompletthetForKøetBehandling(behandling);
    }

    private void vurderKompletthetForKøetBehandling(Behandling behandling) {
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

    public void vurderNyForretningshendelse(Behandling behandling) {
        behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false);
    }

    void spolKomplettBehandlingTilStartpunkt(Behandling behandling, EndringsresultatSnapshot grunnlagSnapshot) {
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

    public KompletthetResultat vurderBehandlingKomplett(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        var åpneAksjonspunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon).collect(Collectors.toList());
        var kompletthetResultat = kompletthetModell.vurderKompletthet(ref, åpneAksjonspunkter);
        return kompletthetResultat;
    }

}
