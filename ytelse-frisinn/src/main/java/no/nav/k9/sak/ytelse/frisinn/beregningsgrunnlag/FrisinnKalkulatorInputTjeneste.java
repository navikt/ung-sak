package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.GrunnbeløpTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class FrisinnKalkulatorInputTjeneste extends KalkulatorInputTjeneste {

    private Boolean toggletVilkårsperioder;

    @Inject
    public FrisinnKalkulatorInputTjeneste(InntektArbeidYtelseTjeneste iayTjeneste,
                                          @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                          GrunnbeløpTjeneste grunnbeløpTjeneste,
                                          @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "true") Boolean toggletVilkårsperioder) {
        super(iayTjeneste, opptjeningForBeregningTjeneste, grunnbeløpTjeneste);
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }

    protected FrisinnKalkulatorInputTjeneste() {
        // for CDI proxy
    }

    @Override
    protected TilKalkulusMapper getTilKalkulusMapper() {
        if (toggletVilkårsperioder) {
            return new FrisinnTilKalkulusMapper();
        }
        return super.getTilKalkulusMapper();
    }

    @Override
    protected LocalDate finnSkjæringstidspunkt(DatoIntervallEntitet vilkårsperiode) {
        return LocalDate.of(2020, 3, 1);
    }

}
