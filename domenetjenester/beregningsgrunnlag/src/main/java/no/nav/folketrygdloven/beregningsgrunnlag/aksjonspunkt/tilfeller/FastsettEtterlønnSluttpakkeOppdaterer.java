package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettEtterlønnSluttpakkeDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_ETTERLØNN_SLUTTPAKKE")
public class FastsettEtterlønnSluttpakkeOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    private static final BigDecimal MÅNEDER_I_ET_ÅR = BigDecimal.valueOf(12);

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        FastsettEtterlønnSluttpakkeDto fastettDto = dto.getFastsettEtterlønnSluttpakke();
        List<BeregningsgrunnlagPrStatusOgAndel> etterlønnSluttpakkeAndeler = finnEtterlønnSluttpakkeAndel(nyttBeregningsgrunnlag);
        Integer fastsattPrMnd = fastettDto.getFastsattPrMnd();
        if (etterlønnSluttpakkeAndeler.isEmpty() || fastsattPrMnd ==null) {
            throw new IllegalStateException("Finner ingen andeler på beregningsgrunnlaget med sluttpakke/etterlønn under fastsetting av inntekt");
        }
        BigDecimal nyVerdiEtterlønnSLuttpakke = BigDecimal.valueOf(fastsattPrMnd).multiply(MÅNEDER_I_ET_ÅR);
        etterlønnSluttpakkeAndeler.forEach(andel -> BeregningsgrunnlagPrStatusOgAndel.builder(andel)
            .medBeregnetPrÅr(nyVerdiEtterlønnSLuttpakke)
            .medFastsattAvSaksbehandler(true));
    }

    private List<BeregningsgrunnlagPrStatusOgAndel> finnEtterlønnSluttpakkeAndel(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        return nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bpsa -> bpsa.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE))
            .collect(Collectors.toList());
    }
}
