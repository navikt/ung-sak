package no.nav.k9.sak.domene.registerinnhenting.task;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

@ApplicationScoped
@ProsessTask(InnhentIAYIAbakusTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentIAYIAbakusTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "innhentsaksopplysninger.abakus";
    public static final String OVERSTYR_KEY = "overstyrt";
    public static final String OVERSTYR_VALUE = "overstyrt";
    public static final String IAY_REGISTERDATA_CALLBACK = "IAY_REGISTERDATA_CALLBACK";
    public static final String OPPDATERT_GRUNNLAG_KEY = "oppdagertGrunnlag";

    private static final Logger LOGGER = LoggerFactory.getLogger(InnhentIAYIAbakusTask.class);
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private RegisterdataInnhenter registerdataInnhenter;

    InnhentIAYIAbakusTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentIAYIAbakusTask(BehandlingRepository behandlingRepository,
                                 ProsessTaskRepository prosessTaskRepository,
                                 RegisterdataInnhenter registerdataInnhenter) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        Optional<String> hendelse = Optional.ofNullable(prosessTaskData.getPropertyValue(ProsessTaskData.HENDELSE_PROPERTY));
        var propertyValue = Optional.ofNullable(prosessTaskData.getPropertyValue(OPPDATERT_GRUNNLAG_KEY));
        if (hendelse.isPresent() && propertyValue.isPresent() && !propertyValue.get().isEmpty()) {
            validerHendelse(prosessTaskData);
            return;
        }

        if (hendelse.isEmpty() || propertyValue.isEmpty()) {
            boolean overstyr = prosessTaskData.getPropertyValue(OVERSTYR_KEY) != null && OVERSTYR_VALUE.equals(prosessTaskData.getPropertyValue(OVERSTYR_KEY));
            Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());

            precondition(behandling);

            if (overstyr) {
                registerdataInnhenter.innhentFullIAYIAbakus(behandling);
                return;
            }
            registerdataInnhenter.innhentIAYIAbakus(behandling);
        }
        settTaskPåVent(prosessTaskData);
    }

    private void precondition(Behandling behandling) {
        BehandlingProsessTask.logContext(behandling);

        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil - saken er ferdig behandlet, kan ikke oppdateres. behandlingId=" + behandling.getId()
                + ", behandlingStatus=" + behandling.getStatus()
                + ", startpunkt=" + behandling.getStartpunkt()
                + ", resultat=" + behandling.getBehandlingResultatType());
        } else {
            LOGGER.info("Innhenter IAY-opplysninger i abakus for behandling: {}", behandling.getId());
        }
    }

    private void validerHendelse(ProsessTaskData prosessTaskData) {

        final var hendelse = prosessTaskData.getPropertyValue(ProsessTaskData.HENDELSE_PROPERTY);
        if (hendelse != null && hendelse.equals(IAY_REGISTERDATA_CALLBACK)) {
            LOGGER.info("Nytt aktivt grunnlag for behandling={} i abakus har uuid={}", prosessTaskData.getBehandlingId(), prosessTaskData.getPropertyValue(OPPDATERT_GRUNNLAG_KEY));
        } else {
            throw new IllegalStateException("Ugyldig hendelse");
        }
    }

    private void settTaskPåVent(ProsessTaskData prosessTaskData) {
        prosessTaskData.setProperty(ProsessTaskData.HENDELSE_PROPERTY, IAY_REGISTERDATA_CALLBACK);
        prosessTaskData.setStatus(ProsessTaskStatus.VENTER_SVAR);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

}
