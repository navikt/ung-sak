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
        String ytelseTypeKode = ytelseType.getKode();
        return FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
    }

    public KalkulatorInputTjeneste getKalkulatorInputTjeneste(FagsakYtelseType ytelseType) {
        String ytelseTypeKode = ytelseType.getKode();
        return FagsakYtelseTypeRef.Lookup.find(kalkulatorInputTjeneste, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + KalkulatorInputTjeneste.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
    }
}
