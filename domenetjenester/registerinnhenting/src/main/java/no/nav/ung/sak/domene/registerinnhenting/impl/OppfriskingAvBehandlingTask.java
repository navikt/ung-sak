package no.nav.ung.sak.domene.registerinnhenting.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

/**
 * Utfører innhenting av registerdata.
 */
@ApplicationScoped
@ProsessTask(OppfriskingAvBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OppfriskingAvBehandlingTask extends UnderBehandlingProsessTask {

    private static final Logger log = LoggerFactory.getLogger(OppfriskingAvBehandlingTask.class);

    public static final String TASKTYPE = "behandlingskontroll.registerdataOppdaterBehandling";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    OppfriskingAvBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public OppfriskingAvBehandlingTask(BehandlingRepositoryProvider repositoryProvider,
                                       BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        var behandlingsId = prosessTaskData.getBehandlingId();
        // NB lås før hent behandling
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingsId);

        if (!behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.INNHENT_REGISTEROPP)) {
            log.info("Behandling har ikke etablert grunnlag, skal ikke innhente registerdata: behandlingId={}", behandlingsId);
            return;
        }

        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
    }
}
