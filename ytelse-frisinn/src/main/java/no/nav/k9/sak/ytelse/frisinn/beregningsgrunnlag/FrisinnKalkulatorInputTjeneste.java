package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class FrisinnKalkulatorInputTjeneste extends KalkulatorInputTjeneste {

    private boolean nyttStpToggle;

    @Inject
    public FrisinnKalkulatorInputTjeneste(@Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                          @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregnings,
                                          @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper,
                                          VilkårResultatRepository vilkårResultatRepository,
                                          @KonfigVerdi(value = "FRISINN_NYTT_STP_TOGGLE", defaultVerdi = "false", required = false) boolean nyttStpToggle) {
        super(opptjeningForBeregningTjeneste, inntektsmeldingerRelevantForBeregnings, ytelseGrunnlagMapper, vilkårResultatRepository, false);
        this.nyttStpToggle = nyttStpToggle;
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
        return nyttStpToggle ? vilkårsperiode.getFomDato() : LocalDate.of(2020, 3, 1);
    }

}
