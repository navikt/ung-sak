package no.nav.k9.sak.dokument.bestill.kafka;

import static no.nav.k9.sak.dokument.bestill.kafka.DokumentbestillerKafkaTaskProperties.OVERSTYRT_MOTTAKER_SEPARATOR;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.bestill.BrevHistorikkinnslag;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.dokument.MottakerDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

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
        bestillBrevMedMottaker(behandling, DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()), bestillBrevDto.getFritekst(), bestillBrevDto.getOverstyrtMottaker(), aktør);
    }

    public void bestillBrevMedMottaker(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, MottakerDto overstyrtMottaker, HistorikkAktør aktør) {
        opprettKafkaTask(behandling, dokumentMalType, overstyrtMottaker, fritekst);
        brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
    }

    public void bestillBrev(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, HistorikkAktør aktør) {
        opprettKafkaTask(behandling, dokumentMalType, null, fritekst);
        brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
    }

    private void opprettKafkaTask(Behandling behandling, DokumentMalType dokumentMalType, MottakerDto overstyrtMottaker, String fritekst) {
        try {
            ProsessTaskData prosessTaskData = new ProsessTaskData(DokumentbestillerKafkaTaskProperties.TASKTYPE);
            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

            prosessTaskData.setPayload(JsonObjectMapper.getJson(fritekst));
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID, behandling.getId().toString());
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());

            if (overstyrtMottaker != null) {
                prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.OVERSTYRT_MOTTAKER,
                    overstyrtMottaker.id+DokumentbestillerKafkaTaskProperties.OVERSTYRT_MOTTAKER_SEPARATOR+overstyrtMottaker.type);
            }
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID, UUID.randomUUID().toString());
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
