package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.GrunnbeløpTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
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

    /**
     * Mapper IAY til kalkuluskontrakt for frisinn. Mapper informasjon for oppgitt vilkårsperiode i tillegg til informasjon før skjæringstidspunktet.
     *
     * @param referanse Behandlingreferanse
     * @param vilkårsperiode Vilkårsperiode
     * @param inntektArbeidYtelseGrunnlag IAY-grunnlag
     * @param oppgittOpptjening OppgittOpptjening
     * @return IAY-grunnlag mappet til kalkuluskontrakt
     */
    @Override
    protected InntektArbeidYtelseGrunnlagDto mapIAYTilKalkulus(BehandlingReferanse referanse, DatoIntervallEntitet vilkårsperiode, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, OppgittOpptjening oppgittOpptjening) {
        if (toggletVilkårsperioder) {
            return new FrisinnTilKalkulusMapper().mapTilDto(inntektArbeidYtelseGrunnlag, referanse.getAktørId(), vilkårsperiode, oppgittOpptjening);
        } else {
            return super.mapIAYTilKalkulus(referanse, vilkårsperiode, inntektArbeidYtelseGrunnlag, oppgittOpptjening);
        }
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
