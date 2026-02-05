package no.nav.ung.sak.hendelsemottak.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandling.revurdering.ÅrsakerOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class HendelsemottakTjeneste {

    private final Logger log = LoggerFactory.getLogger(HendelsemottakTjeneste.class);

    private Instance<FagsakerTilVurderingUtleder> utledere;

    private BehandlingRepository behandlingRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    HendelsemottakTjeneste() {
        // CDI
    }

    @Inject
    public HendelsemottakTjeneste(@Any Instance<FagsakerTilVurderingUtleder> utledere,
                                  BehandlingRepository behandlingRepository,
                                  FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.utledere = utledere;
        this.behandlingRepository = behandlingRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        return finnMatchendeUtledere(hendelse.getHendelseType())
            .stream()
            .map(utleder -> utleder.finnFagsakerTilVurdering(hendelse))
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Fagsak, List<ÅrsakOgPerioder>> mottaHendelse(Hendelse payload) {
        var kandidaterTilRevurdering = finnFagsakerTilVurdering(payload);

        List<String> saksnumre = kandidaterTilRevurdering.keySet().stream().map(f -> f.getSaksnummer().getVerdi()).toList();
        log.info("Mottok hendelse '{}', fant {} relevante fagsaker: {}", payload.getHendelseType(), saksnumre.size(), saksnumre);

        for (Map.Entry<Fagsak, List<ÅrsakOgPerioder>> entry : kandidaterTilRevurdering.entrySet()) {
            var fagsak = entry.getKey();
            var årsakerOgPerioderList = entry.getValue();
            log.info("Oppretter revurdering for fagsak {} med behandlingÅrsaker og perioder {}", fagsak.getSaksnummer().getVerdi(), årsakerOgPerioderList);
            String årsakOgPerioderString = mapTilJsonString(entry.getValue());
            ProsessTaskData tilRevurderingTaskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
            tilRevurderingTaskData.setPayload(årsakOgPerioderString);
            var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
            if (sisteBehandling.isPresent()) {
                Behandling tilRevurdering = sisteBehandling.get();
                tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());
                fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
            } else {
                log.warn("Det var ingen behandling knyttet til {}, ignorerte derfor hendelse av type '{}'", fagsak.getSaksnummer().getVerdi(), payload.getHendelseType());
            }

        }
        return kandidaterTilRevurdering;
    }

    private static String mapTilJsonString(List<ÅrsakOgPerioder> value) {
        String årsakOgPerioderString;
        try {
            årsakOgPerioderString = JsonObjectMapper.getJson(new ÅrsakerOgPerioder(value));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return årsakOgPerioderString;
    }

    private List<FagsakerTilVurderingUtleder> finnMatchendeUtledere(HendelseType hendelseType) {
        var matchendeUtledere = HendelseTypeRef.Lookup.list(utledere, hendelseType.getKode());
        return new ArrayList<>(matchendeUtledere);
    }
}

