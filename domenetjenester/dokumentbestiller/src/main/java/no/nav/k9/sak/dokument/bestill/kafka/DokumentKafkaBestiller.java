package no.nav.k9.sak.dokument.bestill.kafka;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.bestill.BrevHistorikkinnslag;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.dokument.MottakerDto;

@Dependent
public class DokumentKafkaBestiller {
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private BrevHistorikkinnslag brevHistorikkinnslag;

    public DokumentKafkaBestiller() {
        //CDI
    }

    @Inject
    public DokumentKafkaBestiller(BehandlingRepository behandlingRepository,
                                  ProsessTaskRepository prosessTaskRepository,
                                  BrevHistorikkinnslag brevHistorikkinnslag) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.brevHistorikkinnslag = brevHistorikkinnslag;
    }

    public void bestillBrevFraKafka(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        Behandling behandling = behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId());
        DokumentMalType dokumentMalType = DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode());

        var payload = dokumentMalType == DokumentMalType.GENERELL_FRITEKSTBREV ? bestillBrevDto.getFritekstbrev() : bestillBrevDto.getFritekst();

        opprettKafkaTask(behandling, dokumentMalType, bestillBrevDto.getOverstyrtMottaker(), tilJson(payload));
        brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
    }

    private String tilJson(Object payload) {
        try {
            return JsonObjectMapper.getJson(payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void bestillBrev(Behandling behandling, DokumentMalType dokumentMalType, HistorikkAktør aktør) {
        opprettKafkaTask(behandling, dokumentMalType, null, null);
        brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
    }

    private void opprettKafkaTask(Behandling behandling, DokumentMalType dokumentMalType, MottakerDto overstyrtMottaker, String payload) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(DokumentbestillerKafkaTaskProperties.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        prosessTaskData.setPayload(payload);
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID, behandling.getId().toString());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());

        if (overstyrtMottaker != null) {
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.OVERSTYRT_MOTTAKER,
                overstyrtMottaker.id+DokumentbestillerKafkaTaskProperties.OVERSTYRT_MOTTAKER_SEPARATOR+overstyrtMottaker.type);
        }
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID, UUID.randomUUID().toString());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
