package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderEtterlønnSluttpakkeDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_ETTERLØNN_SLUTTPAKKE")
public class VurderEtterlønnSluttpakkeOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        VurderEtterlønnSluttpakkeDto vurderDto = dto.getVurderEtterlønnSluttpakke();
        List<BeregningsgrunnlagPrStatusOgAndel> etterlønnSluttpakkeAndel = finnEtterlønnSluttpakkeAndeler(nyttBeregningsgrunnlag);
        if (!vurderDto.erEtterlønnSluttpakke()) {
            etterlønnSluttpakkeAndel.forEach(andel -> BeregningsgrunnlagPrStatusOgAndel.builder(andel)
                .medFastsattAvSaksbehandler(true)
                .medBeregnetPrÅr(BigDecimal.ZERO));
        }
    }

    private List<BeregningsgrunnlagPrStatusOgAndel> finnEtterlønnSluttpakkeAndeler(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder()
                .get(0)
                .getBeregningsgrunnlagPrStatusOgAndelList()
                .stream()
                .filter(bpsa -> bpsa.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE))
                .collect(Collectors.toList());
    }
}
