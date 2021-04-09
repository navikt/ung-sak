package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandling.prosessering.task.TilbakeTilStartBehandlingTask;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomSamletVurdering;

@ApplicationScoped
@ProsessTask(VurderRevurderingAndreSøknaderTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class VurderRevurderingAndreSøknaderTask implements ProsessTaskHandler {
    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private FagsakTjeneste fagsakTjeneste;
    private Behandlingsoppretter behandlingsoppretter;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;
    private ProsessTaskRepository prosessTaskRepository;



    public static final String TASKTYPE = "iverksetteVedtak.vurderRevurderingAndreSøknader";

    @Inject
    public VurderRevurderingAndreSøknaderTask(BehandlingRepository behandlingRepository, SykdomGrunnlagRepository sykdomGrunnlagRepository, SykdomVurderingRepository sykdomVurderingRepository, FagsakTjeneste fagsakTjeneste, Behandlingsoppretter behandlingsoppretter, BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste, ProsessTaskRepository prosessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        Behandling vedtattBehandling = behandlingRepository.hentBehandling(behandlingId);
        SykdomGrunnlagBehandling vedtattSykdomGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(vedtattBehandling.getUuid()).get();
        LocalDateTimeline<SykdomSamletVurdering> vedtattSakTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(vedtattSykdomGrunnlagBehandling.getGrunnlag());

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = sykdomVurderingRepository.hentAlleSaksnummer(pleietrengende);

        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(vedtattSykdomGrunnlagBehandling.getSaksnummer())) {
                SykdomGrunnlagBehandling kandidatSykdomBehandling = sykdomGrunnlagRepository.hentSisteBehandling(kandidatsaksnummer).map(uuid -> sykdomGrunnlagRepository.hentGrunnlagForBehandling(uuid)).get().get();
                LocalDateTimeline<SykdomSamletVurdering> kandidatsakTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(kandidatSykdomBehandling.getGrunnlag());
                LocalDateTimeline<Boolean> overlappendeTidslinje = SykdomSamletVurdering.finnGrunnlagsforskjellerKunOverlappendeTidslinje(vedtattSakTidslinje, kandidatsakTidslinje);

                if (!overlappendeTidslinje.isEmpty()) {
                    final Fagsak fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(kandidatsaksnummer, true).get();
                    final BehandlingÅrsakType behandlingÅrsak = BehandlingÅrsakType.BERØRT_BEHANDLING;

                    RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
                    if (revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
                        Behandling tilRevurdering = behandlingRepository.hentBehandling(kandidatSykdomBehandling.getBehandlingUuid());
                        behandlingsoppretter.opprettRevurdering(tilRevurdering, behandlingÅrsak);
                        behandlingsprosessApplikasjonTjeneste.asynkStartBehandlingsprosess(vedtattBehandling);
                    } else {
                        Behandling tilbakeTilSTartBehandling = finnBehandlingSomKanSendesTilbakeTilStart(kandidatsaksnummer);
                        if (tilbakeTilSTartBehandling == null) {
                            throw new IllegalStateException("Påvist kandidatsak for restart av behandling, men finner ikke behandling for: " + kandidatsaksnummer.getVerdi());
                        }

                        ProsessTaskData tilbakeTilStart = new ProsessTaskData(TilbakeTilStartBehandlingTask.TASKNAME);
                        tilbakeTilStart.setCallIdFraEksisterende();
                        tilbakeTilStart.setBehandling(fagsak.getId(), tilbakeTilSTartBehandling.getId(), fagsak.getAktørId().getId());
                        tilbakeTilStart.setProperty(TilbakeTilStartBehandlingTask.PROPERTY_START_STEG, Boolean.TRUE.toString());
                        prosessTaskRepository.lagre(tilbakeTilStart);
                    }
                }

            }
        }


    }

    private Behandling finnBehandlingSomKanSendesTilbakeTilStart(Saksnummer saksnummer) {
        final List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(b -> !b.erStatusFerdigbehandlet())
            .collect(Collectors.toList());

        if (behandlinger.isEmpty()) {
            return null;
        }
        if (behandlinger.size() > 1) {
            throw new IllegalStateException("Flere åpne behandlinger på én fagsak er ikke støttet i denne tasken ennå.");
        }
        return behandlinger.get(0);
    }
}
