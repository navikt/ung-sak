package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningStegPeriodeFilter;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.ytelse.beregning.OppdaterKvoteTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
public class PSBOppdaterKvoteTjeneste implements OppdaterKvoteTjeneste {

    private BeregningStegPeriodeFilter beregningStegPeriodeFilter;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    public PSBOppdaterKvoteTjeneste() {
        // CDI
    }

    @Inject
    public PSBOppdaterKvoteTjeneste(BeregningStegPeriodeFilter beregningStegPeriodeFilter, BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.beregningStegPeriodeFilter = beregningStegPeriodeFilter;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }


    @Override
    public void oppdaterKvote(BehandlingReferanse referanse) {
        var vurdertePerioder = beregningStegPeriodeFilter.filtrerPerioder(referanse, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG);

        var fastsatteGrunnlag = beregningsgrunnlagTjeneste.hentEksaktFastsatt(referanse, vurdertePerioder.stream().map(PeriodeTilVurdering::getSkjæringstidspunkt).toList());

        var endretKvoteTidslinjePrStp = fastsatteGrunnlag.stream().collect(
            Collectors.toMap(Beregningsgrunnlag::getSkjæringstidspunkt,
                FinnNyKvoteTjeneste::finnNyKvoteTidslinje
            ));


        // TODO: Gjør oppdatering av kvote for ikke-tomme tidslinjer i endretKvoteTidslinjePrStp


    }

}
