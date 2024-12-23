package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

/**
 * Utfører automatisk gjenopptagelse av en behandling som har
 * et åpent aksjonspunkt som er et autopunkt og har en frist som er passert.
 */
@ApplicationScoped
@ProsessTask(GjenopptaBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class GjenopptaBehandlingTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.gjenopptaBehandling";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    GjenopptaBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public GjenopptaBehandlingTask(BehandlingRepository behandlingRepository,
                                   BehandlingLåsRepository behandlingLåsRepository,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {

        var behandlingsId = prosessTaskData.getBehandlingId();
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingsId);

        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }

        behandlendeEnhetTjeneste.sjekkEnhetEtterEndring(behandling).ifPresent(organisasjonsEnhet -> {
            behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, organisasjonsEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "");
        });
    }
}
