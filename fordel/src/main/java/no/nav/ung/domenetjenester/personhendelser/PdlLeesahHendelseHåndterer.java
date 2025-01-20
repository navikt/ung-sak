package no.nav.ung.domenetjenester.personhendelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;

import java.util.UUID;

import static no.nav.ung.domenetjenester.personhendelser.HendelseMapper.toJson;


@Transactional
@ActivateRequestContext
@ApplicationScoped
public class PdlLeesahHendelseHåndterer {

    private PdlLeesahOversetter oversetter;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public PdlLeesahHendelseHåndterer() {
        // CDI
    }

    @Inject
    public PdlLeesahHendelseHåndterer(PdlLeesahOversetter oversetter, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.oversetter = oversetter;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    void handleMessage(Personhendelse personhendelse) {
        setCallIdForHendelse(personhendelse);

        final Hendelse oversattHendelse = oversetter.oversettStøttetPersonhendelse(personhendelse).orElseThrow();

        final var prosessTaskData = ProsessTaskData.forProsessTask(HåndterUngSakHendelseTask.class);
        prosessTaskData.setPayload(toJson(oversattHendelse));
        prosessTaskData.setCallId(MDCOperations.getCallId());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }

    void handleUngSakMessage(Personhendelse personhendelse) {
        setCallIdForHendelse(personhendelse);

        final Hendelse oversattHendelse = oversetter.oversettStøttetPersonhendelse(personhendelse).orElseThrow();

        final var prosessTaskData = ProsessTaskData.forProsessTask(HåndterUngSakHendelseTask.class);
        prosessTaskData.setPayload(toJson(oversattHendelse));
        prosessTaskData.setCallId(MDCOperations.getCallId());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }

    private void setCallIdForHendelse(Personhendelse payload) {
        var hendelsesId = payload.getHendelseId();
        if (hendelsesId == null || hendelsesId.toString().isEmpty()) {
            MDCOperations.putCallId(UUID.randomUUID().toString());
        } else {
            MDCOperations.putCallId(hendelsesId.toString());
        }
    }
}
