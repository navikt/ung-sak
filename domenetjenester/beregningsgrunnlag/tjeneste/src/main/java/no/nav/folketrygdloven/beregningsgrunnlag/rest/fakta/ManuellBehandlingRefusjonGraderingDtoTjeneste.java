package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

@ApplicationScoped
class ManuellBehandlingRefusjonGraderingDtoTjeneste {

    private FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste;

    @Inject
    public ManuellBehandlingRefusjonGraderingDtoTjeneste(FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste) {
        this.refusjonOgGraderingTjeneste = refusjonOgGraderingTjeneste;
    }

    public ManuellBehandlingRefusjonGraderingDtoTjeneste() {
        // For CDI
    }

    boolean skalSaksbehandlerRedigereInntekt(BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                             AktivitetGradering aktivitetGradering,
                                             BeregningsgrunnlagPeriode periode,
                                             Collection<Inntektsmelding> inntektsmeldinger) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream().anyMatch(andelFraSteg -> refusjonOgGraderingTjeneste
                .vurderManuellBehandlingForAndel(periode, andelFraSteg, aktivitetGradering, beregningAktivitetAggregat, inntektsmeldinger).isPresent());
    }

    boolean skalSaksbehandlerRedigereRefusjon(BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                              AktivitetGradering aktivitetGradering,
                                              BeregningsgrunnlagPeriode periode,
                                             Collection<Inntektsmelding> inntektsmeldinger) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream().anyMatch(andelFraSteg -> refusjonOgGraderingTjeneste
            .vurderManuellBehandlingForAndel(periode, andelFraSteg, aktivitetGradering, beregningAktivitetAggregat, inntektsmeldinger).isPresent()
            && RefusjonDtoTjeneste.skalKunneEndreRefusjon(andelFraSteg, aktivitetGradering, inntektsmeldinger));
    }
}
