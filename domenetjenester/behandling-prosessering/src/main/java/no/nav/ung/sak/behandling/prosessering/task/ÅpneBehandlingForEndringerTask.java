package no.nav.ung.sak.behandling.prosessering.task;

import static no.nav.ung.sak.behandling.prosessering.task.ÅpneBehandlingForEndringerTask.TASKTYPE;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class ÅpneBehandlingForEndringerTask extends UnderBehandlingProsessTask {
    public static final String TASKTYPE = "behandlingskontroll.åpneBehandlingForEndringer";

    public static final String START_STEG = "behandlingskontroll.startSteg";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    ÅpneBehandlingForEndringerTask() {
        // for CDI proxy
    }

    @Inject
    public ÅpneBehandlingForEndringerTask(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                          BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        var steg = BehandlingStegType.fraKode(prosessTaskData.getPropertyValue(START_STEG));

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        reaktiverAksjonspunkter(kontekst, behandling, steg);
        behandling.setÅpnetForEndring(true);
        behandlingskontrollTjeneste.behandlingTilbakeføringHvisTidligereBehandlingSteg(kontekst, steg);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
    }

    private void reaktiverAksjonspunkter(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingStegType steg) {
        Set<String> aksjonspunkterFraOgMedStartpunkt = behandlingskontrollTjeneste
            .finnAksjonspunktDefinisjonerFraOgMed(behandling, steg);

        behandling.getAksjonspunkter().stream()
            .filter(ap -> aksjonspunkterFraOgMedStartpunkt.contains(ap.getAksjonspunktDefinisjon().getKode()))
            .filter(ap -> !AksjonspunktType.AUTOPUNKT.equals(ap.getAksjonspunktDefinisjon().getAksjonspunktType()))
            .forEach(ap -> behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(ap), true));
    }
}
