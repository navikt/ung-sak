package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_SN_NY_I_ARBEIDSLIVET")
public class VurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto nyIArbeidslivetDto = dto.getVurderNyIArbeidslivet();
        BeregningsgrunnlagPeriode periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel bgAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(bpsa -> bpsa.getAktivitetStatus().erSelvstendigNæringsdrivende())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Kunne ikke finne BeregningsgrunnlagPrStatusOgAndel for SELVSTENDIG_NÆRINGSDRIVENDE (eller SN i kombinasjon)"));

        BeregningsgrunnlagPrStatusOgAndel.builder(bgAndel).medNyIArbeidslivet(nyIArbeidslivetDto.erNyIArbeidslivet()).build(periode);
    }

}
