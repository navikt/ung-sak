package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;


@ApplicationScoped
public class BeregningStegPeriodeFilter {

    private BehandlingModellRepository behandlingModellRepository;
    private KalkulusStartpunktUtleder kalkulusStartpunktUtleder;

    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;

    @Inject
    public BeregningStegPeriodeFilter(BehandlingModellRepository behandlingModellRepository, KalkulusStartpunktUtleder kalkulusStartpunktUtleder, BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste) {
        this.behandlingModellRepository = behandlingModellRepository;
        this.kalkulusStartpunktUtleder = kalkulusStartpunktUtleder;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
    }

    public BeregningStegPeriodeFilter() {
    }

    public NavigableSet<PeriodeTilVurdering> filtrerPerioder(BehandlingReferanse behandlingReferanse, BehandlingStegType behandlingStegType) {
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(behandlingReferanse);
        var perioderPrStartSteg = kalkulusStartpunktUtleder.utledPerioderPrStartpunkt(behandlingReferanse, perioderTilVurdering);
        var modell = behandlingModellRepository.getModell(behandlingReferanse.getBehandlingType(), behandlingReferanse.getFagsakYtelseType());
        return perioderPrStartSteg.entrySet().stream()
            .filter(e -> modell.erStegAFørStegB(e.getKey(), behandlingStegType) || e.getKey().equals(behandlingStegType))
            .flatMap(e -> e.getValue().stream())
            .collect(Collectors.toCollection(TreeSet::new));
    }


}
