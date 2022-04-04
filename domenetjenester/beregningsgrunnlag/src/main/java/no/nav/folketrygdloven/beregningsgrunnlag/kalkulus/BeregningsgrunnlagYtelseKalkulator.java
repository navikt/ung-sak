package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
public class BeregningsgrunnlagYtelseKalkulator {

    private Instance<KalkulatorInputTjeneste> kalkulatorInputTjeneste;
    private Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper;

    protected BeregningsgrunnlagYtelseKalkulator() {
        // for proxy
    }

    @Inject
    public BeregningsgrunnlagYtelseKalkulator(@Any Instance<KalkulatorInputTjeneste> kalkulatorInputTjeneste,
                                           @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper) {
        this.kalkulatorInputTjeneste = kalkulatorInputTjeneste;
        this.ytelseGrunnlagMapper = ytelseGrunnlagMapper;
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseType).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseType));
    }

    public KalkulatorInputTjeneste getKalkulatorInputTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(kalkulatorInputTjeneste, ytelseType).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + KalkulatorInputTjeneste.class.getName() + " mapper for ytelsetype=" + ytelseType));
    }
}
