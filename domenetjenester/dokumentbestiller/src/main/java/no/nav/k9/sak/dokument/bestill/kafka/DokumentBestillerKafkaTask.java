package no.nav.k9.sak.dokument.bestill.kafka;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling;
import no.nav.k9.formidling.kontrakt.hendelse.kodeverk.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(DokumentbestillerKafkaTaskProperties.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class DokumentBestillerKafkaTask implements ProsessTaskHandler {

    private DokumentbestillingProducer dokumentbestillingProducer;
    private BehandlingRepository behandlingRepository;

    DokumentBestillerKafkaTask() {
        // for CDI proxy
    }

    @Inject
    public DokumentBestillerKafkaTask(DokumentbestillingProducer dokumentbestillingProducer,
                                      BehandlingRepository behandlingRepository) {
        this.dokumentbestillingProducer = dokumentbestillingProducer;
        this.behandlingRepository = behandlingRepository;
    }

    private static FagsakYtelseType mapYtelse(no.nav.k9.kodeverk.behandling.FagsakYtelseType fpsakYtelseKode) {
        return new FagsakYtelseType(fpsakYtelseKode.getKode());
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        dokumentbestillingProducer.publiserDokumentbestillingJson(serialiser(mapDokumentbestilling(prosessTaskData)));
    }

    private Dokumentbestilling mapDokumentbestilling(ProsessTaskData prosessTaskData) {
        Behandling behandling = behandlingRepository
            .hentBehandling(Long.valueOf(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID)));

        Dokumentbestilling dokumentbestillingDto = new Dokumentbestilling();
        dokumentbestillingDto.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        dokumentbestillingDto.setAktørId(behandling.getAktørId().getId());
        dokumentbestillingDto.setArsakskode(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.REVURDERING_VARSLING_ÅRSAK));
        dokumentbestillingDto.setBehandlingUuid(behandling.getUuid());
        dokumentbestillingDto
            .setDokumentbestillingUuid(UUID.fromString(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID)));
        dokumentbestillingDto.setDokumentMal(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE));
        dokumentbestillingDto.setFritekst(JsonObjectMapper.fromJson(prosessTaskData.getPayloadAsString(), String.class));
        dokumentbestillingDto.setHistorikkAktør(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.HISTORIKK_AKTØR));
        dokumentbestillingDto.setYtelseType(mapYtelse(behandling.getFagsakYtelseType()));
        dokumentbestillingDto.setBehandlendeEnhetNavn(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLENDE_ENHET_NAVN));
        return dokumentbestillingDto;
    }

    private String serialiser(Dokumentbestilling dto) {
        try {
            return JsonObjectMapper.getJson(dto);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "Klarte ikke å serialisere historikkinnslag for behandling=" + dto.getBehandlingUuid() + ", mal=" + dto.getDokumentMal(), e); // NOSONAR
        }
    }
}
