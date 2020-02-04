package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.LønnsendringTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@ApplicationScoped
public class VurderLønnsendringTilfelleUtleder implements TilfelleUtleder {

    @Override
    public Optional<FaktaOmBeregningTilfelle> utled(BeregningsgrunnlagInput input,
                                                    BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        var ref = input.getBehandlingReferanse();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagGrunnlag.getBeregningsgrunnlag().orElse(null);
        Objects.requireNonNull(beregningsgrunnlag, "beregningsgrunnlag");
        return LønnsendringTjeneste.brukerHarHattLønnsendringOgManglerInntektsmelding(ref.getAktørId(), beregningsgrunnlag, input.getIayGrunnlag()) ?
            Optional.of(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING) : Optional.empty();
    }

}
