package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class FrisinnKalkulatorInputTjeneste extends KalkulatorInputTjeneste {


    @Inject
    public FrisinnKalkulatorInputTjeneste(@Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                          @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregnings) {
        super(opptjeningForBeregningTjeneste, inntektsmeldingerRelevantForBeregnings);
    }

    protected FrisinnKalkulatorInputTjeneste() {
        // for CDI proxy
    }

    /**
     * Returnerer statisk skjæringstidspunkt for frisinn som er uavhengig av vilkårsperioden.
     *
     * @param vilkårsperiode Vilkårsperiode
     * @return Skjæringstidspunkt for frisinn
     */
    @Override
    protected LocalDate finnSkjæringstidspunkt(DatoIntervallEntitet vilkårsperiode) {
        return LocalDate.of(2020, 3, 1);
    }

}
