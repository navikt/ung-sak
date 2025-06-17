package no.nav.ung.domenetjenester.arkiv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.kodeverk.OmrådeTema;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.sak.typer.JournalpostId;

@Transactional
@ActivateRequestContext
@ApplicationScoped
public class JournalføringHendelseHåndterer {

    private static final MdcExtendedLogContext MDC_EXTENDED_LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private static final Logger log = LoggerFactory.getLogger(JournalføringHendelseHåndterer.class);
    private ProsessTaskTjeneste prosessTaskTjeneste;

    JournalføringHendelseHåndterer() {
        // CDI
    }

    @Inject
    public JournalføringHendelseHåndterer(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void handleMessage(String key, JournalfoeringHendelseRecord payload) {
        setCallIdForHendelse(payload);

        log.info("Mottatt Journalføringhendelse av type={} for [{}]", payload.getHendelsesType(), key.trim());
        var eksisterendeData = ProsessTaskData.forProsessTask(HentDataFraJoarkTask.class);
        eksisterendeData.setCallIdFraEksisterende();
        var melding = new MottattMelding(eksisterendeData);
        melding.setTema(OmrådeTema.fraKodeDefaultUdefinert(payload.getTemaNytt()));
        melding.setBehandlingTema(BehandlingTema.fraOffisiellKode((payload.getBehandlingstema())));
        melding.setJournalPostId(new JournalpostId(Long.toString(payload.getJournalpostId())));
        melding.setJournalføringHendelsetype(JournalføringHendelsetype.fraKode(payload.getHendelsesType()).orElse(null));
        prosessTaskTjeneste.lagre(melding.getProsessTaskData());
    }

    private void setCallIdForHendelse(JournalfoeringHendelseRecord payload) {
        var hendelsesId = payload.getHendelsesId();
        if (hendelsesId == null) {
            log.info("HendelseId er null, generer callId.");
            hendelsesId = UUID.randomUUID().toString();
        }
        MDCOperations.putCallId(hendelsesId);
        MDC_EXTENDED_LOG_CONTEXT.add("journalpostId", payload.getJournalpostId());
    }
}
