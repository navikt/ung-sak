package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengerForAndelUtil;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

@Dependent
public class HentFeriepengeAndelerTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public HentFeriepengeAndelerTjeneste(BeregningsresultatRepository beregningsresultatRepository, BehandlingRepository behandlingRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnAndelerSomKanGiFeriepenger(Collection<Fagsak> fagsaker) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> resultat = LocalDateTimeline.empty();

        List<Behandling> sisteYtelsebehanlingerForFagsakene = fagsaker.stream()
            .flatMap(fagsak -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelseBehandling(fagsak.getId()).stream())
            .toList();

        for (Behandling behandling : sisteYtelsebehanlingerForFagsakene) {
            LocalDateTimeline<Boolean> feriepengerTidslinje = hentFeriepengerTidslinje(behandling);
            LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> medBehandling = feriepengerTidslinje.mapValue(v -> Set.of(new SaksnummerOgSisteBehandling(behandling.getFagsak().getSaksnummer(), behandling.getId())));
            resultat = resultat.crossJoin(medBehandling, TidslinjeUtil::union);
        }
        return resultat;
    }

    private LocalDateTimeline<Boolean> hentFeriepengerTidslinje(Behandling sisteBehandling) {
        Optional<BeregningsresultatEntitet> beregningsresultat = beregningsresultatRepository.hentEndeligBeregningsresultat(sisteBehandling.getId());
        var beregningsresultatPerioder = beregningsresultat.map(MapBeregningsresultatFeriepengerFraVLTilRegel::mapBeregningsresultat).orElse(List.of());
        return FeriepengerForAndelUtil.utledTidslinjerHarAndelSomKanGiFeriepenger(beregningsresultatPerioder);
    }

}
