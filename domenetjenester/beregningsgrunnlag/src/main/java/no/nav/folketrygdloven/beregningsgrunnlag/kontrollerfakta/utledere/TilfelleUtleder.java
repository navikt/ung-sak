package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere;

import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;


/**
 * TilfelleUtleder
 * Utleder et FaktaOmBeregningTilfelle for behandlingen.
 * Instanser av dette interfacet vil bli injeksert inn i FaktaOmBeregningTilfelleTjeneste.
 * Implementasjoner av denne klassen må derfor være injectable for å skulle kunne brukes i utledelsen av aksjonspunkt for
 * kontroller fakta om beregning.
 */
public interface TilfelleUtleder {
    Optional<FaktaOmBeregningTilfelle> utled(BeregningsgrunnlagInput input,
                                             BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag);
}
