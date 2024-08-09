package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.task.TilbakeTilStartBeregningTask;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput.BeregningInputHistorikkTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput.BeregningInputLagreTjeneste;

@ApplicationScoped
public class ForvaltningOverstyrInputBeregning {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BeregningInputHistorikkTjeneste beregningInputHistorikkTjeneste;
    private BeregningInputLagreTjeneste beregningInputLagreTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private BehandlingModellRepository behandlingModellRepository;

    public ForvaltningOverstyrInputBeregning() {
    }

    @Inject
    public ForvaltningOverstyrInputBeregning(
        HistorikkTjenesteAdapter historikkTjenesteAdapter,
        BeregningInputHistorikkTjeneste beregningInputHistorikkTjeneste,
        BeregningInputLagreTjeneste beregningInputLagreTjeneste,
        FagsakProsessTaskRepository fagsakProsessTaskRepository,
        BehandlingModellRepository behandlingModellRepository) {

        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.beregningInputHistorikkTjeneste = beregningInputHistorikkTjeneste;
        this.beregningInputLagreTjeneste = beregningInputLagreTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingModellRepository = behandlingModellRepository;
    }

    public void overstyrInntektsmelding(
        Behandling behandling,
        OverstyrBeregningInputPeriode periode) {
        if (behandling.erStatusFerdigbehandlet()) {
            throw new IllegalArgumentException("Kan ikke utføre overstyring for behandling som er ferdigbehandlet");
        }
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var dto = new OverstyrInputForBeregningDto("begrunnelse", List.of(periode));
        beregningInputLagreTjeneste.lagreInputOverstyringer(behandlingReferanse, dto);
        beregningInputHistorikkTjeneste.lagHistorikk(behandlingReferanse.getId());
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingReferanse.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        var modell = behandlingModellRepository.getModell(behandlingReferanse.getBehandlingType(), behandlingReferanse.getFagsakYtelseType());
        if (modell.erStegAFørStegB(BehandlingStegType.PRECONDITION_BEREGNING, behandling.getAktivtBehandlingSteg())) {
            ProsessTaskData tilbakeTilBeregningTask = ProsessTaskData.forProsessTask(TilbakeTilStartBeregningTask.class);
            tilbakeTilBeregningTask.setBehandling(behandlingReferanse.getFagsakId(), behandlingReferanse.getId(), behandlingReferanse.getAktørId().getId());
            fagsakProsessTaskRepository.lagreNyGruppe(tilbakeTilBeregningTask);
        }
    }

}
