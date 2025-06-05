package no.nav.ung.sak.domene.vedtak.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingTask;
import no.nav.ung.sak.ytelse.kontroll.ManglendeKontrollperioderTjeneste;

@ApplicationScoped
public class VedtakFattetEventObserver {

    public static final String BREVBESTILLING_TASKTYPE = "formidling.brevbestilling";
    private ProsessTaskTjeneste taskTjeneste;
    private ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste;

    public VedtakFattetEventObserver() {
    }

    @Inject
    public VedtakFattetEventObserver(ProsessTaskTjeneste taskTjeneste, ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste) {
        this.taskTjeneste = taskTjeneste;
        this.manglendeKontrollperioderTjeneste = manglendeKontrollperioderTjeneste;
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
           //TODO flytt til formidling pakke
            var gruppe = new ProsessTaskGruppe(opprettTaskForBrevbestilling(event));

            if (erBehandlingAvRettTypeForAbakus(event)) {
                gruppe.addNesteSekvensiell(opprettTaskForPubliseringAvVedtakMedYtelse(event));
            }

            manglendeKontrollperioderTjeneste.lagProsesstaskForRevurderingGrunnetManglendeKontrollAvInntekt(event.getBehandlingId())
                .ifPresent(gruppe::addNesteParallell);

            taskTjeneste.lagre(gruppe);
        }
    }

    private static ProsessTaskData opprettTaskForBrevbestilling(BehandlingVedtakEvent event) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(BrevbestillingTask.class
        );
        prosessTaskData.setBehandling(event.getFagsakId(), event.getBehandlingId());
        return prosessTaskData;
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
