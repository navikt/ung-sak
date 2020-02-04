package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelMerknad;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelResultat;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAksjonspunktDefinisjon;

@ApplicationScoped
public class AksjonspunktUtlederForeslåBeregning {

    @Inject
    public AksjonspunktUtlederForeslåBeregning() {
    }

    protected List<BeregningAksjonspunktResultat> utledAksjonspunkter(@SuppressWarnings("unused") BeregningsgrunnlagInput input,
                                                                      List<RegelResultat> regelResultater) {
        return mapRegelResultater(regelResultater);
    }

    private List<BeregningAksjonspunktResultat> mapRegelResultater(List<RegelResultat> regelResultater) {
        return regelResultater.stream()
            .map(RegelResultat::getMerknader)
            .flatMap(Collection::stream)
            .distinct()
            .map(this::mapRegelMerknad)
            .map(BeregningAksjonspunktResultat::opprettFor)
            .collect(Collectors.toList());
    }

    private BeregningAksjonspunktDefinisjon mapRegelMerknad(RegelMerknad regelMerknad) {
        return BeregningAksjonspunktDefinisjon.fraKode(regelMerknad.getMerknadKode());
    }
}
