package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettMånedsinntektFLDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller.FaktaOmBeregningTilfelleOppdaterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_MAANEDSINNTEKT_FL")
class FastsettBruttoBeregningsgrunnlagFLOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttGrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        FastsettMånedsinntektFLDto fastsettMånedsinntektFLDto = dto.getFastsettMaanedsinntektFL();
        Integer frilansinntekt = fastsettMånedsinntektFLDto.getMaanedsinntekt();
        BigDecimal årsinntektFL = BigDecimal.valueOf(frilansinntekt).multiply(BigDecimal.valueOf(12));
        List<BeregningsgrunnlagPeriode> bgPerioder = nyttGrunnlag.getBeregningsgrunnlagPerioder();
        for (BeregningsgrunnlagPeriode bgPeriode : bgPerioder) {
            BeregningsgrunnlagPrStatusOgAndel bgAndel = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(bpsa -> AktivitetStatus.FRILANSER.equals(bpsa.getAktivitetStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Mangler BeregningsgrunnlagPrStatusOgAndel[FRILANS] for behandling " + behandlingReferanse.getId()));
            BeregningsgrunnlagPrStatusOgAndel.builder(bgAndel)
                .medBeregnetPrÅr(årsinntektFL)
                .medFastsattAvSaksbehandler(true)
                .build(bgPeriode);
        }
    }

}
