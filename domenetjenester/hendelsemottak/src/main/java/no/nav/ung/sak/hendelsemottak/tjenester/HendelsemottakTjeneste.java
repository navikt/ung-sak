package no.nav.ung.sak.hendelsemottak.tjenester;

import java.time.LocalDate;
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
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;

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

    private LocalDate utledDato(LocalDate fagsakSluttdato, Hendelse payload) {
        var hendelseDato = payload.getHendelsePeriode().getTom();
        if (hendelseDato.isBefore(fagsakSluttdato)) {
            return fagsakSluttdato;
        }
        return hendelseDato;
    }

    private List<FagsakerTilVurderingUtleder> finnMatchendeUtledere(HendelseType hendelseType) {
        var matchendeUtledere = HendelseTypeRef.Lookup.list(utledere, hendelseType.getKode());
        return new ArrayList<>(matchendeUtledere);
    }

    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse) {
        var fagsakerMedBehandlingÅrsak = finnMatchendeUtledere(hendelse.getHendelseType())
            .stream()
            .map(utleder -> utleder.finnFagsakerTilVurdering(hendelse))
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fagsakerMedBehandlingÅrsak;
    }

    public Map<Fagsak, BehandlingÅrsakType> mottaHendelse(Hendelse payload) {
        var kandidaterTilRevurdering = finnFagsakerTilVurdering(payload);

        List<String> saksnumre = kandidaterTilRevurdering.keySet().stream().map(f -> f.getSaksnummer().getVerdi()).toList();
        log.info("Mottok hendelse '{}', fant {} relevante fagsaker: {}", payload.getHendelseType(), saksnumre.size(), saksnumre);

        for (Map.Entry<Fagsak, BehandlingÅrsakType> entry : kandidaterTilRevurdering.entrySet()) {
            var fagsak = entry.getKey();
            var behandlingÅrsak = entry.getValue();

            ProsessTaskData tilRevurderingTaskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, behandlingÅrsak.getKode());
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_FOM, payload.getHendelsePeriode().getFom().toString());
            tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_TOM, utledDato(fagsak.getPeriode().getTomDato(), payload).toString());
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

}

