package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;

class BeregningsgrunnlagGrunnlagTestUtil {
    static BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag(BeregningsgrunnlagGrunnlagEntitet grunnlag, BeregningsgrunnlagEntitet beregningsgrunnlag, BeregningsgrunnlagTilstand tilstand) {
        BeregningsgrunnlagGrunnlagEntitet entitet = grunnlag;
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.of(grunnlag))
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(entitet.getBehandlingId(), tilstand);
    }
}
