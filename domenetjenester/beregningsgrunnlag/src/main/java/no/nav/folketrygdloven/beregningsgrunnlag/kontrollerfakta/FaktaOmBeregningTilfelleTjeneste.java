package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.TilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@ApplicationScoped
public class FaktaOmBeregningTilfelleTjeneste {
    private List<TilfelleUtleder> utledere;

    public FaktaOmBeregningTilfelleTjeneste() {
        // For CDI
    }

    @Inject
    public FaktaOmBeregningTilfelleTjeneste(@Any Instance<TilfelleUtleder> tilfelleUtledere) {
        this.utledere = tilfelleUtledere.stream().collect(Collectors.toList());
    }

    public List<FaktaOmBeregningTilfelle> finnTilfellerForFellesAksjonspunkt(BeregningsgrunnlagInput input,
                                                                             BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        List<FaktaOmBeregningTilfelle> tilfeller = new ArrayList<>();
        for (TilfelleUtleder utleder : utledere) {
            utleder.utled(input, beregningsgrunnlagGrunnlag).ifPresent(tilfeller::add);
        }
        return tilfeller;
    }

}
