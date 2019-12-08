package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class BeregningsgrunnlagInputTjeneste extends BeregningsgrunnlagInputFelles {

    protected BeregningsgrunnlagInputTjeneste() {
        //CDI proxy
    }

    @Inject
    public BeregningsgrunnlagInputTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                      InntektArbeidYtelseTjeneste iayTjeneste,
                                      SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                      AndelGraderingTjeneste andelGraderingTjeneste,
                                      OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste, andelGraderingTjeneste, opptjeningForBeregningTjeneste);
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        // FIXME K9 - trengs noe spesifikt for beregningsgrunnlag?
        return new K9BeregningsgrunnlagInput();
    }
}
