package no.nav.ung.domenetjenester.personhendelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.domenetjenester.personhendelser.utils.PersonhendelseUtils;

import java.util.UUID;


@Transactional
@ActivateRequestContext
@ApplicationScoped
public class PdlLeesahHendelseHåndterer {

    private ProsessTaskTjeneste prosessTaskTjeneste;

    public PdlLeesahHendelseHåndterer() {
        // CDI
    }

    @Inject
    public PdlLeesahHendelseHåndterer(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    void håndterHendelse(String key, Personhendelse personhendelse) {
        setCallIdForHendelse(personhendelse);

        final var prosessTaskData = ProsessTaskData.forProsessTask(HåndterPdlHendelseTask.class);
        prosessTaskData.setPayload(PersonhendelseUtils.tilJson(personhendelse));
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
