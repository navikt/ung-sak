package no.nav.ung.sak.hendelsemottak.tjenester.kabal.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.hendelsemottak.tjenester.kabal.kontrakt.KabalBehandlingEvent;
import no.nav.ung.sak.hendelsemottak.tjenester.kabal.kontrakt.KabalBehandlingEvent.Eventtyper;
import no.nav.ung.sak.hendelsemottak.tjenester.kabal.task.MottaVedtakKlageinstansTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


@ApplicationScoped
@ActivateRequestContext
@Transactional
public class KlageinstansVedtaksHendelseHåndterer {

    private static final Logger log = LoggerFactory.getLogger(KlageinstansVedtaksHendelseHåndterer.class);
    private ProsessTaskTjeneste taskRepository;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    KlageinstansVedtaksHendelseHåndterer() {
    }

    @Inject
    public KlageinstansVedtaksHendelseHåndterer(BehandlingRepository behandlingRepository,
                                                ProsessTaskTjeneste taskRepository,
                                                HistorikkinnslagRepository historikkinnslagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.taskRepository = taskRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    void handleMessage(String key, String payload) {
        log.info("Mottatt hendelse fra Kabal med key='{}', payload={}", key, payload);

        if (!payload.contains(FagsakYtelseType.UNGDOMSYTELSE.getKode())
            || (!payload.contains(Eventtyper.KLAGEBEHANDLING_AVSLUTTET)
                && !payload.contains(Eventtyper.BEHANDLING_FEILREGISTRERT)
                && !payload.contains(Eventtyper.ANKEBEHANDLING_AVSLUTTET))) {
                log.info("Ignorerer hendelse fra Kabal da den ikke gjelder k9, en avsluttet behandling, avsluttet ankebehandling eller en feilregistrert behandling");
                return;
        }

        var kabalBehandlingEvent = JsonObjectMapper.fromJson(payload, KabalBehandlingEvent.class);
        var behandlingUuid = UUID.fromString(kabalBehandlingEvent.kildeReferanse());

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid)
            .orElse(null);

        if (behandling == null) {
            log.info("Fant ingen behandling som matchet hendelse fra kabal, UUID={}", kabalBehandlingEvent.behandlingUuid());
            return;
        }

        if ((behandling.erAvsluttet() || behandling.erUnderIverksettelse())
            && !Eventtyper.ANKEBEHANDLING_AVSLUTTET.equals(kabalBehandlingEvent.type())) {
            log.warn("Mottok hendelse fra kabal for vedtak, men klagehehandling var allerede avsluttet.");
            return;
        }

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(MottaVedtakKlageinstansTask.class);


        switch (kabalBehandlingEvent.type()) {
            case Eventtyper.KLAGEBEHANDLING_AVSLUTTET -> {
                var utfall = kabalBehandlingEvent.detaljer().klagebehandlingAvsluttet().utfall().toString();
                taskData.setProperty(MottaVedtakKlageinstansTask.KABAL_REFERANSE, kabalBehandlingEvent.kabalReferanse());
                taskData.setProperty(MottaVedtakKlageinstansTask.KABAL_EVENTTYPE, kabalBehandlingEvent.type());
                taskData.setProperty(MottaVedtakKlageinstansTask.UTFALL, utfall);
            }
            case Eventtyper.BEHANDLING_FEILREGISTRERT -> {
                taskData.setProperty(MottaVedtakKlageinstansTask.KABAL_REFERANSE, kabalBehandlingEvent.kabalReferanse());
                taskData.setProperty(MottaVedtakKlageinstansTask.KABAL_EVENTTYPE, kabalBehandlingEvent.type());
                taskData.setProperty(MottaVedtakKlageinstansTask.FEILREGISTRERING_BEGRUNNELSE, kabalBehandlingEvent.detaljer().behandlingFeilregistrert().reason());
            }
            case Eventtyper.ANKEBEHANDLING_AVSLUTTET -> {
                taskData.setProperty(MottaVedtakKlageinstansTask.KABAL_REFERANSE, kabalBehandlingEvent.kabalReferanse());
                taskData.setProperty(MottaVedtakKlageinstansTask.KABAL_EVENTTYPE, kabalBehandlingEvent.type());
                taskData.setProperty(MottaVedtakKlageinstansTask.UTFALL, kabalBehandlingEvent.detaljer().ankebehandlingAvsluttet().utfall().toString());
            }
        }

        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setCallIdFraEksisterende();

        taskRepository.lagre(taskData);

    }
}
