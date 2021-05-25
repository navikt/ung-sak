package no.nav.k9.sak.hendelsemottak.tjenester;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.hendelser.HendelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.historikk.kafka.json.SerialiseringUtil;
import no.nav.k9.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class FordelHendelseTjeneste {

    private Instance<FagsakerTilVurderingUtleder> utledere;

    private BehandlingRepository behandlingRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    FordelHendelseTjeneste() {
        // CDI
    }

    @Inject
    public FordelHendelseTjeneste(@Any Instance<FagsakerTilVurderingUtleder> utledere,
                                  BehandlingRepository behandlingRepository,
                                  FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.utledere = utledere;
        this.behandlingRepository = behandlingRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    private List<FagsakerTilVurderingUtleder> finnMatchendeUtledere(HendelseType hendelseType) {
        var matchendeUtledere = HendelseTypeRef.Lookup.list(utledere, hendelseType.getKode());
        return matchendeUtledere.stream().collect(Collectors.toList());
    }

    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(AktørId aktørId, HendelseType hendelseType, String payload) {
        //Hendelse hendelse = SerialiseringUtil.deserialiser(payload, Hendelse.class);
        DødsfallHendelse hendelse = SerialiseringUtil.deserialiser(payload, DødsfallHendelse.class);
        var fagsakerMedBehandlingÅrsak = finnMatchendeUtledere(hendelseType)
            .stream()
            .map(utleder -> utleder.finnFagsakerTilVurdering(aktørId, hendelse))
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        return fagsakerMedBehandlingÅrsak;
    }

    public Map<Fagsak, BehandlingÅrsakType> mottaHendelse(AktørId aktørId, HendelseType hendelseType, String payload) {
        var kandidaterTilRevurdering = finnFagsakerTilVurdering(aktørId, hendelseType, payload);

        for (Map.Entry<Fagsak, BehandlingÅrsakType> entry : kandidaterTilRevurdering.entrySet()) {
            var fagsak = entry.getKey();
            var behandlingÅrsak = entry.getValue();

            ProsessTaskData tilRevurderingTaskData = new ProsessTaskData(OpprettRevurderingEllerOpprettDiffTask.TASKNAME);
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, behandlingÅrsak.getKode());
            var tilRevurdering = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
            tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());

            fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
        }
        return kandidaterTilRevurdering;
    }

}

