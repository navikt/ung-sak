package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.math.BigDecimal;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.VurderATogFLiSammeOrganisasjonAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.VurderATogFLiSammeOrganisasjonDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_AT_OG_FL_I_SAMME_ORGANISASJON")
class FastsettMånedsinntektATogFLiSammeOrganisasjonOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        VurderATogFLiSammeOrganisasjonDto vurderATFLISammeOrgDto = dto.getVurderATogFLiSammeOrganisasjon();
        vurderATFLISammeOrgDto.getVurderATogFLiSammeOrganisasjonAndelListe().forEach(dtoAndel ->
        {
            BeregningsgrunnlagPrStatusOgAndel andelIFørstePeriode = finnAndelIFørstePeriode(nyttBeregningsgrunnlag, dtoAndel);
            int årsinntekt = dtoAndel.getArbeidsinntekt() * 12;
            nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
                BeregningsgrunnlagPrStatusOgAndel matchendeAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.equals(andelIFørstePeriode)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Fant ingen mactchende andel i periode med fom " + periode.getBeregningsgrunnlagPeriodeFom()));
                BeregningsgrunnlagPrStatusOgAndel.builder(matchendeAndel)
                    .medBeregnetPrÅr(BigDecimal.valueOf(årsinntekt))
                    .medFastsattAvSaksbehandler(true);
            });
        });
    }

    private BeregningsgrunnlagPrStatusOgAndel finnAndelIFørstePeriode(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, VurderATogFLiSammeOrganisasjonAndelDto dtoAndel) {
        return nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
                    .getBeregningsgrunnlagPrStatusOgAndelList().stream()
                    .filter(bpsa -> bpsa.getAndelsnr().equals(dtoAndel.getAndelsnr()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Fant ikke andel i første periode med andelsnr " + dtoAndel.getAndelsnr()));
    }

}
