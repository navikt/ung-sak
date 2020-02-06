package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.InntektHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.MapTilLønnsendring;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FaktaBeregningLagreDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_AT_OG_FL_I_SAMME_ORGANISASJON")
public class VurderATOgFLISammeOrgHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private InntektHistorikkTjeneste inntektHistorikkTjeneste;

    public VurderATOgFLISammeOrgHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public VurderATOgFLISammeOrgHistorikkTjeneste(InntektHistorikkTjeneste inntektHistorikkTjeneste) {
        this.inntektHistorikkTjeneste = inntektHistorikkTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        inntektHistorikkTjeneste.lagHistorikk(
            tekstBuilder,
            MapTilLønnsendring.mapLønnsendringFraATogFLSammeOrg(dto, nyttBeregningsgrunnlag,
            forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)),
            iayGrunnlag);
    }

}
