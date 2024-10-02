package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;

/**
 * Tjenesten har som hensikt å reinitialisere perioder som har endret vurderingstatus fra "til-vurdering" til "ikke-til-vurdering".
 * <p>
 * Dette gjøres i to steg:
 * 1) Sette referanser tilbake til initiell referanse
 * 2) Sette vilkårsutfall tilbake til initell vilkårsutfall
 */
@Dependent
public class GjenopprettPerioderSomIkkeVurderesTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(GjenopprettPerioderSomIkkeVurderesTjeneste.class);


    private final BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;

    private final BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;


    @Inject
    public GjenopprettPerioderSomIkkeVurderesTjeneste(BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste, BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }

    /**
     * Resetter beregningsgrunnlagreferanser og vilkårsresultat for perioder som ikke er til vurdering lenger i denne behandlingen
     * <p>
     * Rydding i kalkulus gjøres av no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste#deaktiverBeregningsgrunnlagForAvslåttEllerFjernetPeriode(no.nav.k9.sak.behandling.BehandlingReferanse)
     *
     * @param referanse            Behandlingreferanse
     * @param perioderTilVurdering Perioder til vurdering
     */
    public void gjenopprettVedEndretVurderingsstatus(BehandlingReferanse referanse, NavigableSet<PeriodeTilVurdering> perioderTilVurdering) {
        if (referanse.erRevurdering()) {
            // Gjenoppretter BG-referanser
            beregningsgrunnlagTjeneste.gjenopprettReferanserTilInitiellDersomIkkeTilVurdering(referanse, perioderTilVurdering);

            // Gjenopprett vilkårsvurdering
            var intervallerTilVurdering = perioderTilVurdering.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new));
            beregningsgrunnlagVilkårTjeneste.gjenopprettVilkårsutfallVedBehov(referanse, intervallerTilVurdering);
        }
    }




}
