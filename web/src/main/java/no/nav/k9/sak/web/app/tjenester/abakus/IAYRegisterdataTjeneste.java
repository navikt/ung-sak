package no.nav.k9.sak.web.app.tjenester.abakus;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.RegisterdataCallback;
import no.nav.k9.sak.domene.registerinnhenting.task.InnhentIAYIAbakusTask;

@SuppressWarnings("unused")
@Dependent
public class IAYRegisterdataTjeneste {

    private static final Logger log = LoggerFactory.getLogger(IAYRegisterdataTjeneste.class);

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ProsessTaskTjeneste taskTjeneste;

    public IAYRegisterdataTjeneste() {
    }

    /**
     * Standard ctor som injectes av CDI.
     */
    @Inject
    public IAYRegisterdataTjeneste(InntektArbeidYtelseTjeneste iayTjeneste, ProsessTaskTjeneste taskTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.taskTjeneste = taskTjeneste;
    }

    public void håndterCallback(RegisterdataCallback callback) {
        log.info("Mottatt callback fra Abakus etter registerinnhenting for behandlingId={}, eksisterendeGrunnlag={}, nyttGrunnlag={}",
            callback.getBehandlingId(), callback.getEksisterendeGrunnlagRef(), callback.getOppdatertGrunnlagRef());
        final var tasksSomVenterPåSvar = taskTjeneste.finnAlle(InnhentIAYIAbakusTask.TASKTYPE, ProsessTaskStatus.VENTER_SVAR)
            .stream()
            .filter(it -> Objects.equals(InnhentIAYIAbakusTask.TASKTYPE, it.getTaskType()))
            .filter(it -> it.getBehandlingId().equals("" + callback.getBehandlingId()))
            .toList();

        if (tasksSomVenterPåSvar.size() == 1) {
            mottaHendelse(tasksSomVenterPåSvar.get(0), callback.getOppdatertGrunnlagRef());
        } else if (tasksSomVenterPåSvar.isEmpty()) {
            log.info("Mottatt callback hvor ingen task venter på svar... {}", callback);
        } else {
            log.info("Mottatt callback som svarer til flere tasks som venter. callback={}, tasks={}", callback, tasksSomVenterPåSvar);
        }
    }

    private void mottaHendelse(ProsessTaskData task, UUID oppdatertGrunnlagRef) {
        var props = new Properties();
        props.setProperty(InnhentIAYIAbakusTask.OPPDATERT_GRUNNLAG_KEY, oppdatertGrunnlagRef.toString());
        taskTjeneste.mottaHendelse(task, InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK, props);
        log.info("Behandler hendelse {} i task {}, behandling id {}", InnhentIAYIAbakusTask.IAY_REGISTERDATA_CALLBACK, task.getId(), task.getBehandlingId()); //$NON-NLS-1$
    }
}
