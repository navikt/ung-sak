package no.nav.folketrygdloven.beregningsgrunnlag.regelinput;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.kalkulus.request.v1.regelinput.KomprimerRegelInputRequest;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

/**
 * Task som kaller kalkulus og komprimerer input for regelsporinger
 * <p>
 */
@ApplicationScoped
@ProsessTask(KomprimerFlereRegelInputTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class KomprimerFlereRegelInputTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "beregning.komprimerFlereRegelInput";
    public static final String MAX_ANTALL_REKURSIVE_TASKER = "maxAntallTasker";
    public static final String ANTALL_RADER_PR_TASK = "raderPrTask";
    public static final String LØPENR_TASK = "lopenr_task";

    private KalkulusRestKlient kalkulusSystemRestKlient;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public KomprimerFlereRegelInputTask(SystemUserOidcRestClient systemUserOidcRestClient,
                                        @KonfigVerdi(value = "ftkalkulus.url") URI endpoint,
                                        ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.kalkulusSystemRestKlient = new KalkulusRestKlient(systemUserOidcRestClient, endpoint);
    }


    public KomprimerFlereRegelInputTask() {
    }

    public void doTask(ProsessTaskData prosessTaskData) {
        var maksAntalLTaskerRaw = prosessTaskData.getPropertyValue(MAX_ANTALL_REKURSIVE_TASKER);
        var maksAntallTasker = prosessTaskData.getPropertyValue(MAX_ANTALL_REKURSIVE_TASKER) == null ? null : Integer.valueOf(maksAntalLTaskerRaw);
        var antallRaderPrTask = Integer.valueOf(prosessTaskData.getPropertyValue(ANTALL_RADER_PR_TASK));
        var lopenr = prosessTaskData.getPropertyValue(LØPENR_TASK) == null ? 1 : Integer.parseInt(prosessTaskData.getPropertyValue(LØPENR_TASK));
        kalkulusSystemRestKlient.komprimerFlereRegelinput(new KomprimerRegelInputRequest(null, antallRaderPrTask));
        if ((maksAntallTasker == null || lopenr < maksAntallTasker)) {
            startInputKomprimering(lopenr, maksAntallTasker);
        }
    }

    private void startInputKomprimering(Integer lopenr, Integer maksAntallSaker) {
        ProsessTaskData nesteTask = ProsessTaskData.forProsessTask(KomprimerFlereRegelInputTask.class);
        if (maksAntallSaker != null) {
            nesteTask.setProperty(MAX_ANTALL_REKURSIVE_TASKER, maksAntallSaker.toString());
        }
        int nesteLopenr = lopenr + 1;
        nesteTask.setProperty(LØPENR_TASK, Integer.toString(nesteLopenr));
        prosessTaskTjeneste.lagre(nesteTask);
    }

}
