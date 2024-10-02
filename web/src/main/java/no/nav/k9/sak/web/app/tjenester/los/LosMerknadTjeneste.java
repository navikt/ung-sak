package no.nav.k9.sak.web.app.tjenester.los;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.hendelse.EventHendelse;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.BehandlingProsessHendelseMapper;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.ProsessEventKafkaProducer;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.merknad.BehandlingMerknad;
import no.nav.k9.sak.behandlingslager.behandling.merknad.BehandlingMerknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.merknad.BehandlingMerknadType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapperKodeverdiSomStringSerializer;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.behandling.BehandlingProsessHendelse;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Dependent
public class LosMerknadTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingMerknadRepository behandlingMerknadRepository;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BehandlingProsessHendelseMapper behandlingProsessHendelseMapper;
    private final boolean kodeverkSomStringTopics;
    private ProsessEventKafkaProducer kafkaProducer;

    @Inject
    public LosMerknadTjeneste(BehandlingRepository behandlingRepository,
                              BehandlingMerknadRepository behandlingMerknadRepository,
                              HistorikkTjenesteAdapter historikkTjenesteAdapter,
                              BehandlingProsessHendelseMapper behandlingProsessHendelseMapper,
                              ProsessEventKafkaProducer kafkaProducer,
                              @KonfigVerdi(value = "KODEVERK_SOM_STRING_TOPICS", defaultVerdi = "false") boolean kodeverkSomStringTopics
    ) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingMerknadRepository = behandlingMerknadRepository;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingProsessHendelseMapper = behandlingProsessHendelseMapper;
        this.kafkaProducer = kafkaProducer;
        this.kodeverkSomStringTopics = kodeverkSomStringTopics;
    }


    public Optional<BehandlingMerknad> hertMerknader(UUID behandlingUUID) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUUID);
        return behandlingMerknadRepository.hentBehandlingMerknad(behandling.getId());
    }

    public void lagreMerknad(MerknadEndretDto merknadEndret) {
        Behandling behandling = behandlingRepository.hentBehandling(merknadEndret.behandlingUuid());
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Kan ikke sette merknad på behandling hvor saksbehandlingen er avsluttet");
        }

        Set<BehandlingMerknadType> merknaderFør = behandlingMerknadRepository.hentMerknadTyper(behandling.getId());
        behandlingMerknadRepository.registrerMerknadtyper(behandling.getId(), merknadEndret.merknadKoder(), merknadEndret.fritekst());
        Set<BehandlingMerknadType> merknaderEtter = behandlingMerknadRepository.hentMerknadTyper(behandling.getId());

        if (!merknaderFør.containsAll(merknaderEtter)) {
            lagHistorikkinnslag(merknadEndret, HistorikkinnslagType.MERKNAD_NY);
        }
        if (!merknaderEtter.containsAll(merknaderFør)) {
            lagHistorikkinnslag(merknadEndret, HistorikkinnslagType.MERKNAD_FJERNET);
        }

        EventHendelse hastesakEventHendelse;
        if (merknaderEtter.contains(BehandlingMerknadType.HASTESAK) && !merknaderFør.contains(BehandlingMerknadType.HASTESAK)) {
            hastesakEventHendelse = EventHendelse.HASTESAK_MERKNAD_NY;
        } else if (!merknaderEtter.contains(BehandlingMerknadType.HASTESAK) && merknaderFør.contains(BehandlingMerknadType.HASTESAK)) {
            hastesakEventHendelse = EventHendelse.HASTESAK_MERKNAD_FJERNET;
        } else {
            hastesakEventHendelse = null;
        }
        if (hastesakEventHendelse != null) {
            BehandlingProsessHendelse event = behandlingProsessHendelseMapper.getProduksjonstyringEventDto(hastesakEventHendelse, behandling);
            kafkaProducer.sendHendelse(behandling.getId().toString(), dtoTilJsonRuntimeExceptionOnly(event));
            kafkaProducer.flush();
        }
    }

    private void lagHistorikkinnslag(MerknadEndretDto merknad, HistorikkinnslagType historikkinnslagType) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.UDEFINERT)
            .medHendelse(historikkinnslagType, String.join(",", merknad.merknadKoder().stream().map(Enum::name).toList()))
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
