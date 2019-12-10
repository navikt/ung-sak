package no.nav.folketrygdloven.beregningsgrunnlag.ytelse.fp;

    import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.AksjonspunktUtlederFastsettBeregningsaktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.AvklarAktiviteterTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsperiodeTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef
@ApplicationScoped
public class AksjonspunktUtlederFastsettBeregningsaktiviteterFP extends AksjonspunktUtlederFastsettBeregningsaktiviteter {

    AksjonspunktUtlederFastsettBeregningsaktiviteterFP() {
        // For CDI
    }

    @Inject
    public AksjonspunktUtlederFastsettBeregningsaktiviteterFP(AvklarAktiviteterTjeneste avklarAktiviteterTjeneste,
                                                              BeregningsperiodeTjeneste beregningsperiodeTjeneste) {
        super(avklarAktiviteterTjeneste, beregningsperiodeTjeneste);
    }

    @Override
    public List<BeregningAksjonspunktResultat> utledAksjonspunkterFor(BehandlingReferanse behandlingReferanse,
                                                                      BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                      BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                      boolean erOverstyrt,
                                                                      BeregningsgrunnlagInput bgInput) {
        return super.utledAksjonspunkterForFelles(
            beregningsgrunnlag,
            beregningAktivitetAggregat,
            bgInput.getInntektsmeldinger(),
            bgInput.getIayGrunnlag().getAktørYtelseFraRegister(behandlingReferanse.getAktørId()),
            erOverstyrt);
    }
}
