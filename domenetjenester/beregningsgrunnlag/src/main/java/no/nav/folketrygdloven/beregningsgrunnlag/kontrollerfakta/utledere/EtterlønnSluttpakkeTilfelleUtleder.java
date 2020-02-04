package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.EtterlønnSluttpakkeTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@ApplicationScoped
public class EtterlønnSluttpakkeTilfelleUtleder implements TilfelleUtleder {

    @Override
    public Optional<FaktaOmBeregningTilfelle> utled(BeregningsgrunnlagInput input,
                                                    BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        return EtterlønnSluttpakkeTjeneste.skalVurdereOmBrukerHarEtterlønnSluttpakke(beregningsgrunnlagGrunnlag) ?
            Optional.of(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE) : Optional.empty();
    }
}
