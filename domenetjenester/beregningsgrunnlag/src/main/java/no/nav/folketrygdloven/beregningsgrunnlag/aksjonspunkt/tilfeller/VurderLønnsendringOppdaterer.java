package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.VurderLønnsendringDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_LØNNSENDRING")
public class VurderLønnsendringOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        VurderLønnsendringDto lønnsendringDto = dto.getVurdertLonnsendring();
        List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerAndeler = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList).flatMap(Collection::stream)
            .filter(bpsa -> bpsa.getAktivitetStatus().erArbeidstaker())
            .collect(Collectors.toList());

        if (lønnsendringDto.erLønnsendringIBeregningsperioden()) {
            arbeidstakerAndeler.forEach(andel ->{
                BGAndelArbeidsforhold.Builder bgAndelArbeidsforhold = BGAndelArbeidsforhold
                    .builder(andel.getBgAndelArbeidsforhold().orElse(null))
                    .medLønnsendringIBeregningsperioden(true);
                BeregningsgrunnlagPrStatusOgAndel.builder(andel)
                    .medBGAndelArbeidsforhold(bgAndelArbeidsforhold);
            });
        } else {
            arbeidstakerAndeler.forEach(bgAndel ->{
                BGAndelArbeidsforhold.Builder bgAndelArbeidsforhold = BGAndelArbeidsforhold
                    .builder(bgAndel.getBgAndelArbeidsforhold().orElse(null))
                    .medLønnsendringIBeregningsperioden(false);
                BeregningsgrunnlagPrStatusOgAndel.builder(bgAndel)
                    .medBGAndelArbeidsforhold(bgAndelArbeidsforhold);
            });
        }
    }

}
