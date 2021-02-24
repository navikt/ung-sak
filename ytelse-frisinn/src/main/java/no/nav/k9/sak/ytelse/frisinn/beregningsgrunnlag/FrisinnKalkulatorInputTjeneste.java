package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.KonfigVerdi;

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
