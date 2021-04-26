package no.nav.k9.sak.hendelse.vedtak;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.vedtak.VedtakHendelse;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(VurderOmVedtakPåvirkerAndreSakerTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOmVedtakPåvirkerAndreSakerTask implements ProsessTaskHandler {

    public static final String TASKNAME = "iverksetteVedtak.vurderRevurderingAndreSøknader";

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    VurderOmVedtakPåvirkerAndreSakerTask() {
    }

    @Inject
    public VurderOmVedtakPåvirkerAndreSakerTask(BehandlingRepository behandlingRepository,
                                                FagsakRepository fagsakRepository,
                                                FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var vedtakHendelse = JsonObjectMapper.fromJson(prosessTaskData.getPayloadAsString(), VedtakHendelse.class);

        var vurderOmVedtakPåvirkerSakerTjeneste = VurderOmVedtakPåvirkerSakerTjeneste.finnTjenesteHvisStøttet(vedtakHendelse.getFagsakYtelseType());
        if (vurderOmVedtakPåvirkerSakerTjeneste.isEmpty()) {
            return;
        }
        List<Saksnummer> alleSaksnummer = vurderOmVedtakPåvirkerSakerTjeneste.get().utledSakerSomErKanVærePåvirket(vedtakHendelse);

        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            ProsessTaskData tilRevurderingTaskData = new ProsessTaskData(OpprettRevurderingEllerOpprettDiffTask.TASKNAME);
            var fagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();
            var tilRevurdering = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
            tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());

            fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
        }
    }
}
