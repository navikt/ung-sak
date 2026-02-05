package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;

@ApplicationScoped
public class VedtaksbrevbestillingTjeneste {

    private ProsessTaskTjeneste prosessTaskTjeneste;

    public VedtaksbrevbestillingTjeneste() {
    }

    @Inject
    public VedtaksbrevbestillingTjeneste(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void bestillVedtaksbrevFor(Behandling behandling) {
        prosessTaskTjeneste.lagre(opprettTaskForBrevbestilling(behandling));
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
            prosessTaskTjeneste.lagre(opprettTaskForBrevbestilling(event.getBehandling()));
        }
    }

    private static ProsessTaskData opprettTaskForBrevbestilling(Behandling behandling) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(VurderVedtaksbrevTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().toString());
        prosessTaskData.setSaksnummer(behandling.getFagsak().getSaksnummer().toString());
        return prosessTaskData;
    }
}
