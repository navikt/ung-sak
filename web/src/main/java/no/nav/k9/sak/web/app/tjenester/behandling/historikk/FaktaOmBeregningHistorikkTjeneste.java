package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagPeriodeEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.output.OppdaterBeregningsgrunnlagResultat;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;

/**
 * Lager historikk for aksjonspunkter løst i fakta om beregning.
 */
@Dependent
public class FaktaOmBeregningHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private FaktaOmBeregningVurderingHistorikkTjeneste vurderingHistorikkTjeneste;
    private BeregningsgrunnlagVerdierHistorikkTjeneste verdierHistorikkTjeneste;

    public FaktaOmBeregningHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FaktaOmBeregningHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                             FaktaOmBeregningVurderingHistorikkTjeneste vurderingHistorikkTjeneste,
                                             BeregningsgrunnlagVerdierHistorikkTjeneste verdierHistorikkTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.vurderingHistorikkTjeneste = vurderingHistorikkTjeneste;
        this.verdierHistorikkTjeneste = verdierHistorikkTjeneste;
    }


    public void lagHistorikk(Long behandlingId, List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningsgrunnlagResultatList, String begrunnelse) {
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
        oppdaterBeregningsgrunnlagResultatList.forEach(oppdatering -> byggHistorikkForEndring(behandlingId, oppdatering, tekstBuilder));
        tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING).medBegrunnelse(begrunnelse);
        historikkAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private void byggHistorikkForEndring(Long behandlingId, OppdaterBeregningsgrunnlagResultat oppdaterBeregningsgrunnlagResultat, HistorikkInnslagTekstBuilder tekstBuilder) {
        oppdaterBeregningsgrunnlagResultat.getFaktaOmBeregningVurderinger()
            .ifPresent(vurderinger -> vurderingHistorikkTjeneste.lagHistorikkForVurderinger(behandlingId, tekstBuilder, vurderinger));
        oppdaterBeregningsgrunnlagResultat.getBeregningsgrunnlagEndring()
            .ifPresent(endring -> {
                BeregningsgrunnlagPeriodeEndring førstePeriode = endring.getBeregningsgrunnlagPeriodeEndringer().get(0);
                verdierHistorikkTjeneste.lagHistorikkForBeregningsgrunnlagVerdier(behandlingId, førstePeriode, tekstBuilder);
            });
    }

}
