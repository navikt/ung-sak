package no.nav.k9.sak.hendelsemottak.tjenester;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.hendelser.HendelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.typer.AktørId;

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

    private List<FagsakerTilVurderingUtleder> finnMatchendeUtledere(HendelseType hendelseType) {
        var matchendeUtledere = HendelseTypeRef.Lookup.list(utledere, hendelseType.getKode());
        return new ArrayList<>(matchendeUtledere);
    }

    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(AktørId aktørId, Hendelse hendelse) {
        var fagsakerMedBehandlingÅrsak = finnMatchendeUtledere(hendelse.getHendelseType())
            .stream()
            .map(utleder -> utleder.finnFagsakerTilVurdering(aktørId, hendelse))
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fagsakerMedBehandlingÅrsak;
    }

    public Map<Fagsak, BehandlingÅrsakType> mottaHendelse(AktørId aktørId, Hendelse payload) {
        var kandidaterTilRevurdering = finnFagsakerTilVurdering(aktørId, payload);

        log.info("Mottok hendelse '{}', fant {} relevante fagsaker.", payload.getHendelseType(), kandidaterTilRevurdering.keySet().size());

        for (Map.Entry<Fagsak, BehandlingÅrsakType> entry : kandidaterTilRevurdering.entrySet()) {
            var fagsak = entry.getKey();
            var behandlingÅrsak = entry.getValue();

            ProsessTaskData tilRevurderingTaskData =  ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, behandlingÅrsak.getKode());
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_FOM, payload.getHendelsePeriode().getFom().toString());
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_TOM, payload.getHendelsePeriode().getTom().toString());
            var tilRevurdering = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
            tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());

            fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
        }
        return kandidaterTilRevurdering;
    }

}

