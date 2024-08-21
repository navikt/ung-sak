package no.nav.k9.sak.web.app.tjenester.los;

import java.io.IOException;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.hendelse.EventHendelse;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.BehandlingProsessHendelseMapper;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.ProsessEventKafkaProducer;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapperKodeverdiSomStringSerializer;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.behandling.BehandlingProsessHendelse;

@Dependent
public class LosMerknadTjeneste {

    private BehandlingRepository behandlingRepository;
    private LosSystemUserKlient losKlient;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BehandlingProsessHendelseMapper behandlingProsessHendelseMapper;
    private final boolean kodeverkSomStringTopics;
    private final boolean hastemerknadOverKafka;
    private ProsessEventKafkaProducer kafkaProducer;

    @Inject
    public LosMerknadTjeneste(BehandlingRepository behandlingRepository,
                              LosSystemUserKlient losKlient,
                              HistorikkTjenesteAdapter historikkTjenesteAdapter,
                              BehandlingProsessHendelseMapper behandlingProsessHendelseMapper,
                              @KonfigVerdi(value = "KODEVERK_SOM_STRING_TOPICS",defaultVerdi = "false") boolean kodeverkSomStringTopics,
                              @KonfigVerdi(value = "HASTEMERKNAD_LOS_KAFKA_ENABLED",defaultVerdi = "false") boolean hastemerknadOverKafka
    ) {
        this.behandlingRepository = behandlingRepository;
        this.losKlient = losKlient;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingProsessHendelseMapper = behandlingProsessHendelseMapper;
        this.kodeverkSomStringTopics = kodeverkSomStringTopics;
        this.hastemerknadOverKafka = hastemerknadOverKafka;
    }


    public String hentMerknad(UUID behandlingUUID) {
        return losKlient.hentMerknad(behandlingUUID);
    }

    public String lagreMerknad(MerknadEndretDto merknadEndret) {
        Behandling behandling = behandlingRepository.hentBehandling(merknadEndret.behandlingUuid());
        var merknad = losKlient.lagreMerknad(merknadEndret);
        boolean merknadFjernet = merknad == null;
        var historikkType = merknadFjernet ? HistorikkinnslagType.MERKNAD_FJERNET : HistorikkinnslagType.MERKNAD_NY;
        var hendelseType = merknadFjernet ? EventHendelse.HASTESAK_MERKNAD_FJERNET : EventHendelse.HASTESAK_MERKNAD_NY;

        lagHistorikkinnslag(merknadEndret, historikkType);

        if (hastemerknadOverKafka) {
            BehandlingProsessHendelse event = behandlingProsessHendelseMapper.getProduksjonstyringEventDto(hendelseType, behandling);
            kafkaProducer.sendHendelse(behandling.getId().toString(), dtoTilJsonRuntimeExceptionOnly(event));
            kafkaProducer.flush();
        }

        return merknad;

    }

    private void lagHistorikkinnslag(MerknadEndretDto merknad, HistorikkinnslagType historikkinnslagType) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.UDEFINERT)
            .medHendelse(historikkinnslagType, String.join(",", merknad.merknadKoder()))
            .medBegrunnelse(merknad.fritekst());

        Long behandlingId = behandlingRepository.hentBehandling(merknad.behandlingUuid()).getId();
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private String dtoTilJsonRuntimeExceptionOnly(BehandlingProsessHendelse dto) {
        try {
            return dtoTilJson(dto);
        } catch (IOException e) {
            throw new RuntimeException("Konvertering til json feilet", e);
        }
    }

    private String dtoTilJson(BehandlingProsessHendelse dto) throws IOException {
        if (kodeverkSomStringTopics) {
            return JsonObjectMapperKodeverdiSomStringSerializer.getJson(dto);
        } else {
            return JsonObjectMapper.getJson(dto);
        }
    }

}
