package no.nav.k9.sak.behandling.prosessering.task;

import static no.nav.k9.sak.behandling.prosessering.task.ÅpneBehandlingForEndringerTask.TASKTYPE;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

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
            .finnAksjonspunktDefinisjonerFraOgMed(behandling, steg, true);

        behandling.getAksjonspunkter().stream()
            .filter(ap -> aksjonspunkterFraOgMedStartpunkt.contains(ap.getAksjonspunktDefinisjon().getKode()))
            .filter(ap -> !AksjonspunktType.AUTOPUNKT.equals(ap.getAksjonspunktDefinisjon().getAksjonspunktType()))
            .forEach(ap -> behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(ap), true));
    }
}
