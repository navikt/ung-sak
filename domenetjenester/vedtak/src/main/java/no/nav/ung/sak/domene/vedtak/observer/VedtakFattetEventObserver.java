package no.nav.ung.sak.domene.vedtak.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.ung.sak.domene.vedtak.brukerdialog.PubliserSakTilBrukerdialogTask;

@ApplicationScoped
public class VedtakFattetEventObserver {

    private ProsessTaskTjeneste taskTjeneste;

    public VedtakFattetEventObserver() {
    }

    @Inject
    public VedtakFattetEventObserver(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
            if (erBehandlingAvRettTypeForAbakus(event)) {
                taskTjeneste.lagre(opprettTaskForPubliseringAvVedtakMedYtelse(event));
            }
            if (FagsakYtelseType.AKTIVITETSPENGER.equals(event.getBehandling().getFagsakYtelseType())) {
                taskTjeneste.lagre(opprettTaskForVedtaksstatusTilBrukerdialog(event));
            }
        }
    }

    private ProsessTaskData opprettTaskForVedtaksstatusTilBrukerdialog(BehandlingVedtakEvent event) {
        final ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserSakTilBrukerdialogTask.class);
        taskData.setBehandling(event.getFagsakId(), event.getBehandlingId(), event.getAktørId().toString());
        taskData.setCallIdFraEksisterende();
        return taskData;
    }

    private boolean erBehandlingAvRettTypeForAbakus(BehandlingVedtakEvent event) {
        return event.getBehandling().erYtelseBehandling();
    }

    @Deprecated
    private ProsessTaskData opprettTaskForPubliseringAvVedtakMedYtelse(BehandlingVedtakEvent event) {
        final ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserVedtattYtelseHendelseTask.class);
        taskData.setBehandling(event.getFagsakId(), event.getBehandlingId(), event.getAktørId().toString());
        taskData.setCallIdFraEksisterende();
        return taskData;
    }

}
