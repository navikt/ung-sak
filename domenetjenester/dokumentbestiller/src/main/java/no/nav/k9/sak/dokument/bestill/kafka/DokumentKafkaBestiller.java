package no.nav.k9.sak.dokument.bestill.kafka;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.behandling.RevurderingVarslingÅrsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.bestill.BrevHistorikkinnslag;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.util.StringUtils;

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
        RevurderingVarslingÅrsak årsak = null;
        if (!StringUtils.nullOrEmpty(bestillBrevDto.getÅrsakskode())) {
            årsak = RevurderingVarslingÅrsak.fraKode(bestillBrevDto.getÅrsakskode());
        }
        Behandling behandling = behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId());
        bestillBrev(behandling, bestillBrevDto.getBrevmalkode(), bestillBrevDto.getFritekst(), årsak, aktør);
    }

    public void bestillBrev(Behandling behandling, String dokumentMalKode, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør) {
        bestillBrev(behandling, DokumentMalType.fraKode(dokumentMalKode), fritekst, årsak, aktør);
    }

    public void bestillBrev(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør) {
        opprettKafkaTask(behandling, dokumentMalType, fritekst, årsak, aktør);
        brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
    }

    @SuppressWarnings("removal")
    private void opprettKafkaTask(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør) {
        try {
            ProsessTaskData prosessTaskData = new ProsessTaskData(DokumentbestillerKafkaTaskProperties.TASKTYPE);
            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            
            prosessTaskData.setPayload(JsonObjectMapper.getJson(fritekst));
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID, behandling.getId().toString());
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.REVURDERING_VARSLING_ÅRSAK, årsak != null ? årsak.getKode() : null);
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.HISTORIKK_AKTØR, aktør.getKode());
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID, UUID.randomUUID().toString());
            prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BEHANDLENDE_ENHET_NAVN, behandling.getBehandlendeOrganisasjonsEnhet().getEnhetNavn());
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
